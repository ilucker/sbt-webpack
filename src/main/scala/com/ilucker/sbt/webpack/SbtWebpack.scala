package com.ilucker.sbt.webpack


import com.typesafe.sbt.jse.JsTaskImport.JsTaskKeys._
import com.typesafe.sbt.jse.SbtJsEngine.{autoImport => sbtJsAutoImport}
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import.{Plugin => _, _}
import com.typesafe.sbt.web.SbtWeb.{autoImport => sbtWebAutoImport}
import com.typesafe.sbt.web.incremental.{OpResult, OpSuccess}
import com.typesafe.sbt.web.{CompileProblems, LineBasedProblem, incremental}
import sbt.Keys._
import sbt._
import sbt.classpath.ClasspathUtilities
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString}
import xsbti.Problem

import scala.collection.immutable.Set

object Import {

  object WebpackKeys {
    val webpack = taskKey[Seq[File]]("Invoke the Webpack.")
    val webpackConfig = settingKey[Option[File]]("The webpack configuration file.")
  }

}


object SbtWebpack extends AutoPlugin {


  override def requires = SbtJsTask
  override def trigger = AllRequirements

  val autoImport = Import

  import autoImport.WebpackKeys._
  import sbtWebAutoImport.WebKeys._
  import sbtWebAutoImport._


  // @formatter:off

  override def projectSettings =
    inTask(webpack)(
      SbtJsTask.jsTaskSpecificUnscopedSettings ++ //shellSource, (fileInputHasher, resourceManaged)/(Assets,TestAssets)
      Seq(
        moduleName := webpack.key.label,
        shellFile := getClass.getClassLoader.getResource("webpack.js")
      )
    ) ++
    SbtJsTask.addJsSourceFileTasks(webpack) ++
    Seq(
      webpackConfig := None,
      webpack in Assets <<= Webpack(Assets).dependsOn(nodeModules in Plugin, nodeModules in Assets, webModules in Assets),
      webpack in TestAssets <<= Webpack(TestAssets).dependsOn(nodeModules in Plugin, nodeModules in Assets, webModules in TestAssets)
    )
  // @formatter:on
}

object SbtWebpackEmbedded extends AutoPlugin {

  override def requires = SbtWebpack
  override def trigger = NoTrigger

  object autoImport {
    val webpackDependencies = settingKey[Seq[ModuleID]]("Declares webpack dependencies.")
  }


  import autoImport._
  import sbtWebAutoImport._
  import SbtWebpack.autoImport.WebpackKeys._

  override def projectSettings = Seq(
    webpackDependencies := Seq(
      "org.webjars.npm" % "ripemd160" % "0.2.1" % Plugin.toString(),
      "org.webjars.npm" % "component-indexof" % "0.0.3" % Plugin.toString(),
      "org.webjars.npm" % "webpack" % "1.12.14" % Plugin.toString() exclude("org.webjars.npm", "indexof")
    ),

    ivyConfigurations += Plugin,
    libraryDependencies <++= webpackDependencies,
    webJarsClassLoader in Plugin := {
      val finder = PathFinder(Classpaths.managedJars(Plugin, classpathTypes.value, update.value).map(_.data))
      ClasspathUtilities.toLoader(finder, (webJarsClassLoader in Plugin).value)
    },
    nodeModuleDirectories in(Assets, webpack) <<= nodeModuleDirectories in Plugin,
    nodeModules in(Assets, webpack) <<= nodeModules in Plugin,

    nodeModuleDirectories in(TestAssets, webpack) <<= nodeModuleDirectories in Plugin,
    nodeModules in(TestAssets, webpack) <<= nodeModules in Plugin
  )
}

object Webpack {

  object WebpackProtocol extends DefaultJsonProtocol {

    import SbtJsTask.JsTaskProtocol._

    case class WebpackResultItem(filesRead: Set[File], filesWritten: Set[File], problems: Seq[LineBasedProblem])

    case class WebpackResults(filesRead: Set[File], filesWritten: Set[File], problems: Seq[LineBasedProblem]) {
      def this() = this(Nil.toSet, Nil.toSet, Nil)
      def add(wp: WebpackResultItem): WebpackResults = WebpackResults(
        filesRead = filesRead ++ wp.filesRead,
        filesWritten = filesWritten ++ wp.filesWritten,
        problems = problems ++ wp.problems
      )
    }

    implicit val webpackResultItemFormat = jsonFormat3(WebpackResultItem)
  }

  import SbtWebpack.autoImport.WebpackKeys._
  import sbtJsAutoImport.JsEngineKeys._
  import sbtWebAutoImport.WebKeys._

  def apply(config: Configuration): Def.Initialize[Task[Seq[File]]] = Def.task {

    val configFile = (webpackConfig in config).value.getOrElse((baseDirectory in(config, webpack)).value / "webpack-config.js").absolutePath
    val rootDirectories = (Keys.sourceDirectories in(config, webpack)).value ++
      (unmanagedResourceDirectories in (config, webpack)).value ++
      (nodeModuleDirectories in config).value
    val sources = (Keys.sources in(config, webpack)).value ++ (unmanagedResources in (config, webpack)).value

    implicit val opInputHasher = (fileInputHasher in(config, webpack)).value
    val results: (Set[File], Seq[Problem]) = incremental.syncIncremental((streams in config).value.cacheDirectory / "run", sources) {
      modifiedSources: Seq[File] =>

        if (modifiedSources.isEmpty) {
          (Map.empty[File, OpResult], Nil)
        } else {

          streams.value.log.info(s"Webpack running on ${sources.size} source(s) (${modifiedSources.size} modified) ")

          val options = JsObject(
            "sourceDirectory" -> JsString((sourceDirectory in(config, webpack)).value.absolutePath),
            "rootDirectories" -> JsArray(rootDirectories.map(d => JsString(d.absolutePath)).toVector),
            "targetDirectory" -> JsString((resourceManaged in(config, webpack)).value.absolutePath)
          ).toString()

          val resultJs = SbtJsTask.executeJs(state.value,
            (engineType in webpack).value,
            (command in webpack).value,
            (nodeModuleDirectories in(config, webpack)).value.map(_.getCanonicalPath),
            (shellSource in(config, webpack)).value,
            Seq(configFile, options),
            (timeoutPerSource in(config, webpack)).value
          )

          import WebpackProtocol._
          val webpackResults: WebpackResults = resultJs.foldLeft(new WebpackResults()) {
            (cumulative, result) => cumulative.add(result.convertTo[WebpackResultItem])
          }

          val op = OpSuccess(webpackResults.filesRead, webpackResults.filesWritten)
          (modifiedSources.map(_ -> op).toMap, webpackResults.problems)
        }
    }
    val (filesWritten, problems) = results
    CompileProblems.report((reporter in webpack).value, problems)

    filesWritten.toSeq

  }
}
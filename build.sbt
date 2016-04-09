import sbt.Keys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Version

lazy val root = Project("sbt-webpack", file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt).
  settings(
    sbtPlugin := true,

    description := "sbt plugin to run webpack module bundler",
    licenses +=("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    organization := "com.ilucker",
    developers := List(
      Developer("ilucker", "Alexander Gavrilov", "@ilucker", url("https://github.com/ilucker"))
    ),
    scmInfo := Some(ScmInfo(url("https://github.com/ilucker/sbt-webpack"), "git@github.com:ilucker/sbt-webpack.git")),

    //Compiler
    scalaVersion := "2.10.6",
    scalacOptions in Compile ++= Seq("-deprecation", "-target:jvm-1.7"),

    //Dependencies
    addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.1.3"),

    //Tests
    ScriptedPlugin.scriptedSettings,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false,

    //Publish
    publishMavenStyle := false,
    bintrayOrganization := None,
    bintrayRepository := "sbt-plugins",

    //Release
    git.useGitDescribe := true,
    releaseVersion := { ver => Version(ver).map(_.withoutQualifier.bump(releaseVersionBump.value).string).getOrElse("0.1.0") },
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      runClean,
      runTest,
      releaseStepInputTask(scripted),
      tagRelease,
      publishArtifacts,
      pushChanges
    ),

    //write release version into git-ignored folder
    releaseVersionFile := target.value / ".version",

    SettingKey[String]("show-release-version") := releaseVersion.value(version.value),
    SettingKey[String]("show-next-version") := releaseNextVersion.value(releaseVersion.value(version.value))
  )


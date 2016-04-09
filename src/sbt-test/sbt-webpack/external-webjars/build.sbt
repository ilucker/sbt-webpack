import WebpackKeys._
import WebKeys._

lazy val `external-webjars` = (project in file(".")).enablePlugins(SbtWeb).settings(
  scalaVersion := "2.11.8",

  libraryDependencies += "org.webjars.npm" % "camelcase" % "2.0.1"
)

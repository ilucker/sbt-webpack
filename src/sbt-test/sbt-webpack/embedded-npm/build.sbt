import WebpackKeys._
import WebKeys._

lazy val `embedded-npm` = (project in file(".")).enablePlugins(SbtWebpackEmbedded).settings(
  scalaVersion := "2.11.8"
)

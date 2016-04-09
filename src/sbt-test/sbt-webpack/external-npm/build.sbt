import WebpackKeys._
import WebKeys._

lazy val `external-npm` = (project in file(".")).enablePlugins(SbtWeb).settings(
  scalaVersion := "2.11.8"
)

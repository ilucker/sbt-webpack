libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value


addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

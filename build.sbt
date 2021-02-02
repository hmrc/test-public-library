import sbt.Keys.crossScalaVersions
import sbt._

val name = "test-public-library"

lazy val library = Project(name, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion                     := 0,
    makePublicallyAvailableOnBintray := true
  ).settings(
    scalaVersion        := "2.12.10",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    scalacOptions       ++= Seq("-deprecation"),
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

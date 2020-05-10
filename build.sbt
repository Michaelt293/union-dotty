import xerial.sbt.Sonatype._

val dottyVersion = "0.24.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "union-dotty",
    organization := "io.github.michaelt293",
    licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    description := "Error handling with union types in Dotty",
    version := "0.1.0",

    sonatypeProjectHosting := Some(GitHubHosting("Michaelt293", "union-dotty", "Michaelt293@gmail.com")),

    // publish to the sonatype repository
    publishTo := sonatypePublishTo.value,

    scalaVersion := dottyVersion,

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
  )

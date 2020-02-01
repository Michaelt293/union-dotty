val dottyVersion = "0.21.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "union-dotty",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
  )

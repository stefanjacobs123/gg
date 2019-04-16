scalaVersion     := "2.12.8"
version          := "0.1.0-SNAPSHOT"
organization     := "com.br"

lazy val root = (project in file("."))
  .settings(
    name := "gg",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.22",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )

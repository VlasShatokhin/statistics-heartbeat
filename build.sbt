name := "statistics-heartbeat"

version := "0.1"

scalaVersion := "2.12.5"

val akka = "2.5.11"
val akkaHttp = "10.1.0"
val scalaTest = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttp,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttp,
  "com.typesafe.akka" %% "akka-actor" % akka,
  "com.typesafe.akka" %% "akka-stream" % akka,
  "org.scalatest" %% "scalatest" % scalaTest % Test,
  "com.typesafe.akka" %% "akka-testkit" % akka % Test)

import sbt.Keys.libraryDependencies

val _organization = "name.mtkachev"
val _scalaVersion = "2.12.4"
//val akkaVersion = "2.5.10dbg"
val akkaVersion = "2.5.10"

lazy val commonSettings = Seq(
  organization := _organization,
  
  version := "0.0.1",
  resolvers ++= Seq(
      Resolver.mavenLocal
    , Resolver.sonatypeRepo("releases")
    , Resolver.sonatypeRepo("snapshots")
    , "Bintray " at "https://dl.bintray.com/projectseptemberinc/maven"
  ),
  scalaVersion := _scalaVersion,


  libraryDependencies ++= Seq(
      "com.typesafe.akka"            %% "akka-actor"                    % akkaVersion,
      "com.typesafe.akka"            %% "akka-persistence"              % akkaVersion,
      "com.typesafe.akka"            %% "akka-stream"                   % akkaVersion,
      "com.typesafe.akka"            %% "akka-http"                     % "10.1.0",
      "com.typesafe.akka"            %% "akka-http-spray-json"          % "10.1.0",
      "com.typesafe.scala-logging"   %% "scala-logging"                 % "3.7.2"
    )
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "dump2018-stream-exmpl",
    scalacOptions ++= Seq(
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:postfixOps",
      "-Ypartial-unification",
      "-deprecation"
    )
  )


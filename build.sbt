  name := "scala_play_api"
 
version := "1.0"
      
lazy val `scala_play_api` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  jdbc ,
  ehcache ,
  ws ,
  specs2 % Test ,
  guice,
  "com.typesafe.play"       %%  "play-slick"                        % "3.0.0" ,
  "org.postgresql"          %   "postgresql"                        % "42.2.2",
  "org.scalatestplus.play"  %% "scalatestplus-play"                 % "3.0.0"       % "test",
  "com.faunadb"             %% "faunadb-scala"                      % "2.2.0",
  "com.google.cloud"        % "google-cloud-spanner"                % "0.53.0-beta"

)



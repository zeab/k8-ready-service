//Imports
import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  //Add the library's to this list that need to be excluded. Below is excluding certain log4j lib's
  val removeDependencies: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }
  )
  
  //List of Versions
  val V = new {
    val akka                        = "2.5.14"
    val akkaHttp                    = "10.1.3"
    val akkaHttpCirce               = "1.21.0"
    val circe                       = "0.9.3"
    val scalaTest                   = "3.0.5"
    val aenea                       = "1.0.0"
    val scalaextras                 = "1.1.0"
    val datastax                    = "3.4.0"
    val zooKeeper                   = "3.4.14"
    val kafka                       = "2.2.0"
  }

  //List of Dependencies
  val D = new {
    //Akka
    val akkaStream                  = "com.typesafe.akka" %% "akka-stream" % V.akka
    //Akka Http
    val akkaHttp                    = "com.typesafe.akka" %% "akka-http" % V.akkaHttp
    //Json
    val circeCore                   = "io.circe" %% "circe-parser" % V.circe
    val circeParser                 = "io.circe" %% "circe-generic" % V.circe
    val akkaHttpCirce               = "de.heikoseeberger" %% "akka-http-circe" % V.akkaHttpCirce
    //Logging
    val akkaSlf4j                   = "com.typesafe.akka" %% "akka-slf4j" % V.akka
    //Test
    val scalaTest                   = "org.scalatest" %% "scalatest" % V.scalaTest % "test"
    val akkaTestKit                 = "com.typesafe.akka" %% "akka-testkit" % V.akka % Test
    //Xml
    val aenea                       = "com.github.zeab" %% "aenea" % V.aenea
    //Scala Extras
    val scalaExtras                 = "com.github.zeab" %% "scalaextras" % V.scalaextras
    //Cassandra
    val datastax                    = "com.datastax.cassandra" % "cassandra-driver-extras" % V.datastax
    //ZooKeeper
    val zooKeeper                   = "org.apache.zookeeper" % "zookeeper" % V.zooKeeper
    //Kafka
    val kafka                       = "org.apache.kafka" % "kafka-clients" % V.kafka
  }

  val rootDependencies: Seq[ModuleID] = Seq(
    D.akkaStream,
    D.akkaHttp,
    D.circeCore,
    D.circeParser,
    D.akkaHttpCirce,
    D.akkaSlf4j,
    D.scalaTest,
    D.akkaTestKit,
    D.aenea,
    D.scalaExtras,
    D.datastax,
    D.zooKeeper,
    D.kafka
  )

}

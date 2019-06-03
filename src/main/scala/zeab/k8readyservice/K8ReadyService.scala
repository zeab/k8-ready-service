package zeab.k8readyservice

//Imports
import zeab.k8readyservice.shutdownhook.ShutdownHook
import zeab.k8readyservice.webservice.Routes
import zeab.k8readyservice.zookeeper.StubWatcher
import zeab.scalaextras.logging.Logging
//Java
import java.util.Properties
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Kafka
import org.apache.kafka.clients.producer.KafkaProducer
//ZooKeeper
import org.apache.zookeeper.ZooKeeper
//Scala
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object K8ReadyService extends App with Logging {

  val serviceName:String = "K8ReadyService"
  log.info(s"Starting $serviceName")

  //Akka
  implicit val system: ActorSystem = ActorSystem(serviceName, ConfigFactory.load())
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  log.info("ActorSystem started")

  //Cassandra
  val cassandraHost: String = getEnvVar[String]("CASSANDRA_HOST", "127.0.0.1")
  val cassandraPort: Int = getEnvVar[Int]("CASSANDRA_PORT", 0)
  val cassandraUsername:String = getEnvVar[String]("CASSANDRA_USERNAME", "")
  val cassandraPassword:String = getEnvVar[String]("CASSANDRA_PASSWORD", "")
  implicit val cluster: Cluster =
    if (cassandraPassword != "" && cassandraUsername != "")
      Cluster.builder
        .addContactPoint(cassandraHost)
        .withCredentials(cassandraUsername, cassandraPassword)
        .build
    else
      Cluster.builder
        .addContactPoint(cassandraHost)
        .build

  implicit val session: Session = Try(cluster.connect()) match {
    case Success(connectedSession) =>
      log.info(s"Cassandra Session open")
      connectedSession
    case Failure(ex) =>
      ShutdownHook.forceQuit(ex, 1, cluster, mat, system)
      throw new Exception("System has exited")
  }

  val zooKeeperHost: String = getEnvVar[String]("ZOOKEEPER_HOST", "localhost")
  val zooKeeperPort: String = getEnvVar[String]("ZOOKEEPER_PORT", "2181")
  implicit val zooKeeper: ZooKeeper = new ZooKeeper(s"$zooKeeperHost:$zooKeeperPort", 10000, new StubWatcher)

  //Kafka
  val kafkaHost: String = getEnvVar[String]("KAFKA_HOST", "localhost")
  val kafkaPort: String = getEnvVar[String]("KAFKA_PORT", "9092")
//  val kafkaConsumerGroupId: String = getEnvVar[String]("KAFKA_CONSUMER_GROUP_ID")
//  val kafkaConsumerGroups: List[String] = getEnvVar[String]("KAFKA_CONSUMER_GROUPS", ',', None)
  //Kafka - Producer
  val producerProps: Properties = new Properties()
  producerProps.put("bootstrap.servers", s"$kafkaHost:$kafkaPort")
  producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  implicit val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](producerProps)
  log.info("Kafka Producer open")
  //Kafka - Consumer
//  val  consumerProps: Properties = new Properties()
//  consumerProps.put("bootstrap.servers", s"$kafkaHost:$kafkaPort")
//  consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
//  consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
//  consumerProps.put("group.id", kafkaConsumerGroupId)
//  implicit val consumer: KafkaConsumer[String, String] =
//    new KafkaConsumer[String, String](consumerProps)
//  consumer.subscribe(kafkaConsumerGroups.asJavaCollection)
//  val consumerPoll: Future[Unit] =
//    Future{
//      while(true){
//        val records: Iterable[ConsumerRecord[String, String]] =
//          consumer.poll(1.second.toMillis).asScala
//        records.foreach{ msg => system.eventStream.publish(msg) }
//      }
//    }

  //Http Server
  val webServiceHost: String = getEnvVar[String]("WEB_SERVICE_HOST", "0.0.0.0")
  val webServicePort: Int = getEnvVar[Int]("WEB_SERVICE_PORT", 8080)
  val webServerSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = webServiceHost, webServicePort)
  implicit val binding: Future[Http.ServerBinding] =
    webServerSource.to(Sink.foreach { connection =>
      log.info("Accepted new connection from {}", connection.remoteAddress)
      connection.handleWith(Routes.checkRoute("live"))
    }).run()
  log.info(s"Http Server is now online at http://$webServiceHost:$webServicePort")

  binding.onComplete{
    case Success(_) => ShutdownHook.quit
    case Failure(ex) => ShutdownHook.forceQuit(
      ex, 1, zooKeeper, producer, session, cluster, mat, system, binding, ec
    )
  }

}

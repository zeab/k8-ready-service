package zeab.k8readyservice.shutdownhook

//Imports
import zeab.scalaextras.logging.Logging
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Kafka
import org.apache.kafka.clients.producer.KafkaProducer
//ZooKeeper
import org.apache.zookeeper.ZooKeeper
//Scala
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.sys.ShutdownHookThread

object ShutdownHook extends Logging {

  def quit(implicit
           zooKeeper:ZooKeeper,
           producer: KafkaProducer[String, String],
           session:Session,
           cluster:Cluster,
           mat:ActorMaterializer,
           system:ActorSystem,
           binding: Future[Http.ServerBinding],
           ec:ExecutionContext): Unit ={
    log.info("Termination of service requested")
    scala.sys.addShutdownHook {
      log.info("Terminating...")
      binding
        .flatMap(_.unbind())
        .onComplete { _ =>
          zooKeeper.close()
          producer.close()
          session.close()
          cluster.close()
          mat.shutdown()
          system.terminate()
        }
      Await.result(system.whenTerminated, 60.seconds)
      log.info("Terminated...Exiting")
    }
  }

  def forceQuit(
                 ex:Throwable,
                 exitCode:Int,
                 zooKeeper:ZooKeeper,
                 producer: KafkaProducer[String, String],
                 session:Session,
                 cluster:Cluster,
                 mat:ActorMaterializer,
                 system:ActorSystem,
                 binding: Future[Http.ServerBinding],
                 ec:ExecutionContext
               ): Unit ={
    log.error(ex.toString)
    ShutdownHookThread.apply{
      log.info("Terminating... ")
      cluster.close()
      zooKeeper.close()
      session.close()
      mat.shutdown()
      log.info("Terminated...Exiting")
      system.terminate()
    }
    System.exit(exitCode)
  }

  def forceQuit(
                 ex:Throwable,
                 exitCode:Int,
                 cluster:Cluster,
                 mat:ActorMaterializer,
                 system:ActorSystem
               ): Unit ={
    log.error(ex.toString)
    ShutdownHookThread.apply{
      log.info("Terminating... ")
      cluster.close()
      mat.shutdown()
      log.info("Terminated...Exiting")
      system.terminate()
    }
    System.exit(exitCode)
  }

}

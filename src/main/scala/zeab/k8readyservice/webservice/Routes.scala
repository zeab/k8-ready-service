package zeab.k8readyservice.webservice

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.server.Directives.{complete, extractActorSystem, extractExecutionContext, extractLog, get, onComplete, pathPrefix}
import akka.http.scaladsl.server.Route
import com.datastax.driver.core.Session
import org.apache.zookeeper.ZooKeeper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
//Scala
import scala.collection.JavaConverters._

object Routes extends DirectiveExtensions {

  //Used for the readiness and liveness checks
  def checkRoute(path: String)(implicit zooKeeper: ZooKeeper, session: Session): Route =
    extractExecutionContext { implicit ec: ExecutionContext =>
      extractLog { log: LoggingAdapter =>
        logRoute{
          pathPrefix(path) {
            get {
              val isCassandraConnected: Future[Boolean] =
                Future {
                  if (session.getState.getConnectedHosts.asScala.toList.nonEmpty) true
                  else false
                }
              val isZooKeeperConnected: Future[Boolean] =
                Future {
                  zooKeeper.getState.toString match {
                    case "CONNECTED" => true
                    case _ => false
                  }
                }
              val isKafkaConnected: Future[Boolean] =
                Future {
                  val brokers: List[String] =
                    Try(zooKeeper.getChildren("/brokers/ids", false).asScala.toList) match {
                      case Success(value) => value
                      case Failure(_) => List.empty
                    }
                  if (brokers.isEmpty) false
                  else true
                }
              val check: Future[Boolean] =
                for {
                  cas <- isCassandraConnected
                  zoo <- isZooKeeperConnected
                  kaf <- isKafkaConnected
                } yield {
                  val checkResults: Boolean = cas & kaf & zoo
                  if (true) log.info(s"$path check results | cas: $cas | zoo: $zoo | kaf: $kaf")
                  else log.error(s"$path check results | cas: $cas | zoo: $zoo | kaf: $kaf")
                  checkResults
                }
              onComplete(check) {
                case Success(checkResult) =>
                  if (checkResult) complete(OK, s"$path check passed")
                  else complete(InternalServerError, s"$path check failed")
                case Failure(_) => complete(InternalServerError, s"$path check failed")
              }
            }
          }
        }
      }
    }

}

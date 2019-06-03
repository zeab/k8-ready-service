package zeab.k8readyservice.webservice

//Imports
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, RejectionHandler, RequestContext}
import zeab.k8readyservice.udp.UdpMsg

//TODO actually correct the udp data grams
trait DirectiveExtensions {

  val logRoute: Directive[Unit] =
    extractActorSystem.flatMap{system: ActorSystem =>
      extractRequestContext.flatMap{ctx: RequestContext =>
        val startTime: Long = System.currentTimeMillis()
        val correlationId: String = ctx.request.headers
          .find(_.name() == "correlationId")
          .map(_.value())
          .getOrElse("no-correlation-id")
        mapResponse{resp =>
          val totalTime: Long = System.currentTimeMillis() - startTime
          ctx.log.info(s"correlationId: $correlationId [${resp.status.intValue()}] ${ctx.request.method.name} ${ctx.request.uri} took: ${totalTime}ms")
          //TODO This is where the udp stuff goes
          if (200 until 299 contains resp.status.intValue()) system.eventStream.publish(UdpMsg("service"))
          else  system.eventStream.publish(UdpMsg("service"))
          resp
        } & handleRejections{ RejectionHandler.default }
      }
    }

}

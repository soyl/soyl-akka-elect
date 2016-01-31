package io.soyl.elect

import java.util.UUID
import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import io.soyl.elect.LockToucher.TouchLock
import scala.concurrent.duration._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * This actor is responsible for touching the lock of a singleton
  * leader periodically to prevent it from timing out.
  *
  * The most critical thing this actor needs to make sure is that
  * it reports a failure to touch the lock using the onFailure callback.
  */
class LockToucher(id: UUID, cfg: LockInfo, ec: SingletonContext,
                  onFailure: Throwable => Unit) extends Actor with LazyLogging {

  import context._

  private val TouchInterval = (cfg.lockttlsec / 2).seconds

  override def preStart() = {
    super.preStart()
    logger.info(s"Starting LockToucher for '${cfg.name}'")
    Try(system.scheduler.scheduleOnce(TouchInterval, self, TouchLock))
      .recover { case NonFatal(e) => stopOnError(s"Scheduling initial TouchLock for '${cfg.name}' failed.", e) }
  }

  override def postRestart(reason: Throwable) = {}

  def receive = {
    case TouchLock =>
      ec.touchLock(cfg.lockttlsec, id, cfg.name)(dispatcher).map { case _ =>
        logger.info(s"Touched lock named '${cfg.name}'")
        system.scheduler.scheduleOnce(TouchInterval, self, TouchLock)
      } recover { case NonFatal(e) => stopOnError(s"Touching lock for '${cfg.name}' failed: ${e.getMessage}", e) }
    case _ => ()
  }

  /**
    * Report error through callback and stop the actor.
    *
    * @param msg
    * @param cause
    */
  private def stopOnError(msg: String, cause: Throwable): Unit = {
    logger.error(msg, cause)
    onFailure(cause)
    context.stop(self)
  }

}

object LockToucher {

  def props(id: UUID, cfg: LockInfo, ec: SingletonContext, onFailure: Throwable => Unit): Props
  = Props(new LockToucher(id, cfg, ec, onFailure))

  private[LockToucher] case object TouchLock


}

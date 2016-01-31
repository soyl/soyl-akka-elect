package io.soyl.elect

import java.util.{Date, UUID}

import io.soyl.elect.Worker.Start

import akka.actor.SupervisorStrategy.{Decider, Stop}
import akka.actor._
import org.joda.time.{DateTimeZone, DateTime}
import com.typesafe.scalalogging.LazyLogging
import scala.reflect.ClassTag
import scala.util.{Success, Failure, Random, Try}
import scala.util.control.NonFatal
import scala.concurrent.duration._

class SingletonManager(workerProps: Props, name: String, ec: SingletonContext,
                       config: SingletonManagerConfig) extends Actor with LazyLogging {

  import context._
  import SingletonManager._

  val id = UUID.randomUUID()
//  var lockToucher: Option[ActorRef] = None
//  var worker: Option[ActorRef] = None

  // Randomize start delay so not all singleton managers hit lock at the same time
  val DurationUntilStart = randomDuration(config.initialMaxDelay)

  // We want Worker and lock toucher to be stopped in case of any failure
  override val supervisorStrategy: SupervisorStrategy = {
    def defaultDecider: Decider = {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Stop
    }
    AllForOneStrategy()(defaultDecider)
  }

  override def preStart(): Unit = {
    super.preStart()
    system.scheduler.scheduleOnce(DurationUntilStart, self, InspectLock)
  }


  def receive = trying

  /**
    *
    * @return
    */
  def trying: Receive = {

    case InspectLock =>
      logger.info(s"Inspecting lock information for '$name'")
      ec.getLockInfo(name).map {
        case Some(lockInfo) if shouldTryLock(utcNow, lockInfo) =>
          logger.info(s"Trying lock is indicated by '$lockInfo'")
          self ! TryLock(lockInfo)
        case Some(lockInfo) =>
          logger.info(s"Trying the lock not indicated by $lockInfo")
          system.scheduler.scheduleOnce(lockInfo.checksec.seconds, self, InspectLock)
        case None =>
          logger.warn(s"Lock row for '$name' not found, initializing the row with default values.")
          initLock(config)
      } recover { case NonFatal(e) =>
        logger.error(s"Cannot get lock metadata '$name': ${e.getMessage}; next inspect in ${config.checkInterval}", e)
        system.scheduler.scheduleOnce(config.checkInterval, self, InspectLock)
      }

    case TryLock(lockInfo) =>
      logger.info(s"Trying to get lock '${lockInfo.name}' for leader")
      ec.tryLock(lockInfo.lockttlsec, id, lockInfo.name).map {
        case false =>
          logger.info(s"Lock '${lockInfo.name}' got taken before we could take it")
          system.scheduler.scheduleOnce(lockInfo.checksec.seconds, self, InspectLock)
        case true =>
          logger.info(s"Obtained lock '$name'; starting LockToucher and Worker")
          val lockToucher = context.actorOf(LockToucher.props(id, lockInfo, ec,
            (cause: Throwable) => self ! AbortFailure(cause)),
            SingletonManager.toucherName(name))
          val worker = context.actorOf(workerProps,SingletonManager.workerName(name))
          become(running(lockToucher, worker, lockInfo))
          worker ! Start(lockInfo.state)
          // FIXME Now we are going 'silent', we could also start an info-loop 'I have lock'

      } recover { case NonFatal(e) =>
        logger.error(s"Cannot get lock '${lockInfo.name}': ${e.getMessage}", e)
        system.scheduler.scheduleOnce(lockInfo.checksec.seconds, self, InspectLock)
      }
    case _ => ()

  }

  /**
    *
    * @param lockToucher
    * @param worker
    * @param lockInfo
    * @return
    */
  def running(lockToucher: ActorRef, worker: ActorRef, lockInfo: LockInfo): Receive = {

    case Commit(state: String) =>
      // intermediate save state without updating date of last run
      logger.info("_____________________________________ LEADER: Commit")
      ec.commitState(state, name).onComplete {
        case Failure(e) =>
          Try(context.stop(worker))
          Try(context.stop(lockToucher))
          logger.error(s"An error occurred when committing state '$state' for lock '$name': ${e.getMessage}. Not resuming to inspection.", e)
        // FIXME keep trying or not? currently actors will al stop -> maybe become(failed)
        case Success(_) =>
          logger.info(s"Committed for lock '$name' state '$state'; worker still running")
          // Nothing to do here, since worker is still running.
      }

    case Done(state: String) =>
      // save state and update last
      logger.info("_____________________________________ LEADER: Done")
      ec.storeState(state, name).onComplete {
        case Failure(e) =>
          Try(context.stop(worker))
          Try(context.stop(lockToucher))
          logger.error(s"An error occurred when setting state '$state' for lock '$name': ${e.getMessage}. Not resuming to inspection.", e)
        // FIXME keep trying or not? currently actors will al stop -> maybe become(failed)
          // set vars to none!!! make func: clean
        case Success(_) =>
          Try(context.stop(worker))
          Try(context.stop(lockToucher))
          logger.info(s"Work done for lock '$name' with state '$state'")
          system.scheduler.scheduleOnce(lockInfo.checksec.seconds, self, InspectLock)
          become(trying)
      }


    case AbortFailure(cause) =>
      Try(context.stop(worker))
      Try(context.stop(lockToucher))
      logger.error(s"An error occurred for singleton '$name': ${cause.getMessage}. Not resuming to inspection.", cause)
    // FIXME keep trying or not? currently actors will al stop -> maybe become(failed)

    case _ => ()
  }

  private def initLock(config: SingletonManagerConfig): Unit = {
    ec.initLock(name, config.checkInterval, config.lockTtl, config.lockEnabled).onComplete {
      case Success(applied) if applied =>
        logger.info(s"Initialized lock row for lock '$name'")
        system.scheduler.scheduleOnce(config.checkInterval, self, InspectLock)
      case Success(applied) if !applied =>
        logger.info(s"Lock '$name' seems to have been initialized by peer service")
        system.scheduler.scheduleOnce(config.checkInterval, self, InspectLock)
      case Failure(NonFatal(e)) =>
        logger.error(s"Error initializing lock row for '$name': ${e.getMessage}", e)
        system.scheduler.scheduleOnce(config.checkInterval, self, InspectLock)
    }
  }

  // FIXME: postStop() to send poison pill to worker for graceful shutdown?

  override def postStop(): Unit = {
    logger.info("_____________________________________ LEADER: postStop")
    super.postStop()
  }

}

object SingletonManager extends LazyLogging {

  def apply[T <: Worker : ClassTag](name: String, sc: SingletonContext, config: SingletonManagerConfig)(implicit as: ActorSystem) = {
    val workerProps: Props = Props[T]()

    as.actorOf(Props(new SingletonManager(workerProps, name, sc, config)),leaderName(name))
  }

  def apply[T <: Worker : ClassTag](creator: => T, name: String, sc: SingletonContext, config: SingletonManagerConfig)(implicit as: ActorSystem) = {
    val workerProps: Props = Props(creator)
    as.actorOf(Props(new SingletonManager(workerProps, name, sc, config)))
  }


  private[elect] case class Done(state: String)

  private[elect] case class Commit(state: String)

  private[elect] case class AbortFailure(cause: Throwable)


  private[SingletonManager] def utcNow: DateTime = new DateTime(DateTimeZone.UTC)

  private[SingletonManager] def leaderName(name: String) = s"Singleton-$name-Leader"

  private[SingletonManager] def toucherName(name: String) = s"Singleton-$name-Toucher"

  private[SingletonManager] def workerName(name: String) = s"Singleton-$name-Worker"

  private[SingletonManager] case object InspectLock

  private[SingletonManager] case class TryLock(lockInfo: LockInfo)

  private[SingletonManager] def randomDuration(max: FiniteDuration): FiniteDuration = {
    new Random().nextInt(max.toSeconds.toInt).seconds
  }

  /**
    *
    * http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
    *
    * @param now
    * @param lockInfo
    * @return
    */
  private[SingletonManager] def shouldTryLock(now: DateTime, lockInfo: LockInfo): Boolean = {
    if (lockInfo.enabled && lockInfo.lock.isEmpty) {
      lockInfo.last match {
        case None => true
        case Some(last) =>
          lockInfo.schedule match {
            case None =>
              // Try lock if we have no schedule and last is in the past
              last.getMillis < now.getMillis
            case Some(schedule) =>
              //val last = lockInfo.last.getOrElse(utcNow)
              val nextRunDate: Date = schedule.getNextValidTimeAfter(last.toDate)
              //println(s"====> last: $last, ${last.toDate} next: $nextRunDate  now: $now")
              // Is work due? (Is next due date in the past?)
              now.isAfter(nextRunDate.getTime - 1)
          }
      }

    } else
      false
  }


}



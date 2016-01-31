package io.soyl.elect.jobs

import com.typesafe.scalalogging.LazyLogging
import io.soyl.elect.Worker

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class B(n: Int) extends Worker with LazyLogging {


  logger.info("************************ ctpr" + n)

  override def run(state: Option[String])(implicit ex: ExecutionContext): Future[String] = {
    logger.info("############################################### Running")
    logger.info("############################################### DONE ")
    Future.successful("next")
  }
}

class NoOpWorker extends Worker with LazyLogging {

  override def run(state: Option[String])(implicit ex: ExecutionContext): Future[String] = {
    if(state.isEmpty) throw new IllegalArgumentException("NoOpWorker needs initial state")
    Future.successful(state.get)
  }
}

class NoOpDelayedWorker(delay:FiniteDuration,fail:Boolean = false) extends Worker with LazyLogging {

  override def run(state: Option[String])(implicit ex: ExecutionContext): Future[String] = {
    if(state.isEmpty) throw new IllegalArgumentException("NoOpWorker needs initial state")
    Future {
      Thread.sleep(delay.toMillis)
      if(fail) throw new RuntimeException(s"NoOpDelayedWorker with fail=$fail failed after $delay")
      else state.get
    }
  }
}

class NoOpDelayedCommittingWorker(delay:FiniteDuration,fail:Boolean = false) extends Worker with LazyLogging {

  override def run(state: Option[String])(implicit ex: ExecutionContext): Future[String] = {
    if(state.isEmpty) throw new IllegalArgumentException("NoOpWorker needs initial state")
    Future {
      Thread.sleep(delay.toMillis / 2)
      commit(state.get)
      Thread.sleep(delay.toMillis / 2)
      if(fail) throw new RuntimeException(s"NoOpDelayedWorker with fail=$fail failed after $delay")
      else state.get
    }
  }
}



class Counter extends Worker with LazyLogging {

  override def run(state: Option[String])(implicit ex: ExecutionContext): Future[String] = {
    logger.info(s"Counter got $state")
    val c = state.map(Integer.valueOf).getOrElse(0)
    Future.successful(String.valueOf(c))
  }
}


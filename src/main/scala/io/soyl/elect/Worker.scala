package io.soyl.elect

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import io.soyl.elect.SingletonManager.{Commit, Done, AbortFailure}
import io.soyl.elect.Worker.Start

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * Base class for workers to be managed by singleton manager.
  *
  * FIXME: Examples.
  *
  */
abstract class Worker extends Actor with LazyLogging {


  def receive = {
    case Start(state) =>
      context.become(running)
      import context.dispatcher
      run(state).map { case maybeNewState =>
        context.parent ! Done(maybeNewState)
      } recover { case NonFatal(e) =>
        context.parent ! AbortFailure(e)
      }
    case _ => ()
  }

  def running: Receive = {
    case _ => ()
  }

  /**
    * Workers can call this method to mark in the lock data that their
    * activity has been processed up until a certain 'safe point'. Should
    * the worker fail subsequently, processing will resume using the state
    * provided to the commit call.
    *
    * @param state The state to commit
    */
  protected final def commit(state: String): Unit = {
    context.parent ! Commit(state)
  }

  /**
    * Workers need to override this method with their own intended behavior.
    *
    * The run method is executed in the worker's context.dispatcher execution
    * context.
    *
    * @param maybeState
    * @param ex
    * @return The new state that is the result of the worker's activity as [[Option]].
    *         Some(value) indicates
    */
  protected def run(maybeState: Option[String])(implicit ex: ExecutionContext): Future[Option[String]]

}

object Worker {

  /** The [[io.soyl.elect.SingletonManager]] sends this message to worker instance
    * to trigger start and to supply state found associated with lock.
    */
  private[elect] case class Start(maybeState: Option[String])

}
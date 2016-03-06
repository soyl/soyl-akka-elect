package io.soyl.elect

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import io.soyl.elect.SingletonManager.{AbortFailure, Commit, Done}
import io.soyl.elect.Worker.Start
import io.soyl.elect.jobs.{NoOpDelayedCommittingWorker, NoOpDelayedWorker, NoOpWorker}
import io.soyl.elect.testutils.ParentProbe
import org.scalatest._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._

class WorkerSpec(_system: ActorSystem) extends TestKit(_system)
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEachTestData with MockitoSugar {

  val TestState = "TestState"

  def this() = this(ActorSystem("xxxLockToucherSpec"))

  "A Worker" must {
    "Send 'Done' message to parent with correct state when run() immediately returns." in {
      lazy val p = ParentProbe(Props[NoOpWorker])
      p.sendViaParent(Start(Some(TestState)))
      p.expectMsg(Done(Some(TestState)))
    }

    "Send 'Done' message to parent with correct state when run() executes asynchronously." in {
      lazy val p = ParentProbe(Props(new NoOpDelayedWorker(1.seconds)))
      p.sendViaParent(Start(Some(TestState)))
      p.expectMsg(2.second, Done(Some(TestState)))
    }

    "Send 'Commit' and 'Done' message to parent with correct state when run() calls commit()." in {
      lazy val p = ParentProbe(Props(new NoOpDelayedCommittingWorker(2.seconds)))
      p.sendViaParent(Start(Some(TestState)))
      p.expectMsg(2.second, Commit(TestState))
      p.expectMsg(4.second, Done(Some(TestState)))
    }

    "Send 'AbortFailure' message to parent when run() throws an exception" in {
      lazy val p = ParentProbe(Props(new NoOpDelayedWorker(1.seconds, fail = true)))
      p.sendViaParent(Start(Some(TestState)))
      p.expectMsgClass(classOf[AbortFailure])
    }
  }

}


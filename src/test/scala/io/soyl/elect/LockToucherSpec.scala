package io.soyl.elect

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import io.soyl.elect.jobs.NoOpWorker
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.Eventually._
import org.scalatest.time.{Millis, Span}
import org.scalatest._
import scala.concurrent.Future
import scala.concurrent.duration._
import org.mockito.Matchers._
import org.mockito.Mockito._

class LockToucherSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEachTestData with MockitoSugar {

  def this() = this(ActorSystem("LockToucherSpec"))

  implicit val patienceConfig = new PatienceConfig(scaled(Span(10000, Millis)))

  lazy val id = UUID.fromString("14c73860-cc5f-40e7-af8f-812e54545cc3")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var aut: ActorRef = null

  override def afterEach(testData: TestData): Unit = {
    system.stop(aut)
  }

  "A LockToucher actor" must {
    "cancel a job if the database update of the lock TTL fails" in {
      var cancelled = false
      val context = mock[SingletonContext]
      val li = LockInfo("test", 10, true, Some(id), 2, None, None, None)
      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.failed(new RuntimeException("Test-Exception")))
      aut = system.actorOf(LockToucher.props(id, li, context, (t:Throwable) => cancelled = true))
      eventually {
        cancelled should be(true)
      }
    }

    "periodically invoke the database query to prolong the lock TTL" in {
      val context = mock[SingletonContext]
      val li = LockInfo("test", 10, true, Some(id), 4, None, None, None)
      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.successful(()))
      val a:TestActorRef[LockToucher] = TestActorRef(LockToucher.props(id, li, context, (t: Throwable) => ()))

      eventually {
        verify(context,times(2)).touchLock(li.lockttlsec, id, li.name)(a.underlyingActor.context.dispatcher)
      }

    }
  }

//  "A LockToucher actor" must {
//    "cancel a job if the database update of the lock TTL fails" in {
//      var cancelled = false
//      val context = mock[SingletonContext]
//      //when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.successful(()))
//      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.failed(new RuntimeException("")))
//
//      //      val s = SingletonManager[A]("fooar",context,SingletonManagerConfig(lockEnabled = true,initialMaxDelay = 1.second))
//
//      //aut = system.actorOf(KeepJobLockedActor.props(lockRepository, JobType1, UUIDs.timeBased(), 1 second, () => cancelled = true))
//      //  id: UUID, cfg: LockInfo, ec:SingletonContext,
//      //  onFailure: (String, Throwable) => Unit
//      //      class LockToucher(id: UUID, cfg: LockInfo, ec:SingletonContext,
//      //                        onFailure: (String, Throwable) => Unit) extends Actor with LazyLogging {
//
//      val li = LockInfo("test", 10, true, Some(id), 2, None, None, None)
//      aut = system.actorOf(LockToucher.props(id, li, context, (s: String, t: Throwable) => {
//        println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
//        cancelled = true
//      }))
//
//      eventually {
//        cancelled should be(true)
//      }
//    }

}

//import java.util.UUID
//
//import akka.actor.{ActorRef, ActorSystem}
//import akka.testkit.{ImplicitSender, TestKit}
//import com.datastax.driver.core.utils.UUIDs
//import org.mockito.Matchers._
//import org.mockito.Mockito._
//import org.scalatest.mock.MockitoSugar
//
//import scala.concurrent.Future
//import scala.language.postfixOps

//class KeepJobLockedActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEachTestData with MockitoSugar {
//
//  def this() = this(ActorSystem("KeepJobLockedSpec"))
//
//  implicit val patienceConfig = new PatienceConfig(scaled(Span(5000, Millis)))
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  var aut: ActorRef = null
//
//  override def afterEach(testData: TestData): Unit = {
//    system.stop(aut)
//  }
//
//  "An KeepJobLocked actor" must {
//    "cancel a job if it loses its lock" in {
//      var cancelled = false
//      val lockRepository = mock[LockRepository]
//      when(lockRepository.updateLock(any[JobType], any[UUID], any[Duration])(any())).thenReturn(Future.successful(false))
//      aut = system.actorOf(KeepJobLockedActor.props(lockRepository, JobType1, UUIDs.timeBased(), 1 second, () => cancelled = true))
//
//      eventually {
//        cancelled should be (true)
//      }
//    }
//
//    "cancel a job if working on lockRepo fails" in {
//      var cancelled = false
//      val lockRepository = mock[LockRepository]
//      when(lockRepository.updateLock(any[JobType], any[UUID], any[Duration])(any())).thenThrow(new RuntimeException("Simulated Exception"))
//      aut = system.actorOf(KeepJobLockedActor.props(lockRepository, JobType1, UUIDs.timeBased(), 1 second, () => cancelled = true))
//
//      eventually {
//        cancelled should be (true)
//      }
//    }
//
//    "cancel a job if communication with c* fails" in {
//      var cancelled = false
//      val lockRepository = mock[LockRepository]
//      when(lockRepository.updateLock(any[JobType], any[UUID], any[Duration])(any())).thenReturn(Future.failed(new RuntimeException("Simulated Exception")))
//      aut = system.actorOf(KeepJobLockedActor.props(lockRepository, JobType1, UUIDs.timeBased(), 1 second, () => cancelled = true))
//
//      eventually {
//        cancelled should be (true)
//      }
//    }
//  }
//}

package io.soyl.elect

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.soyl.elect.SingletonManager._
import io.soyl.elect.jobs.NoOpWorker
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}
import scala.concurrent.duration._

import scala.concurrent.Future

class SingletonManagerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEachTestData with MockitoSugar {

  def this() = this(ActorSystem("SingletonManagerSpec"))

  implicit val patienceConfig = new PatienceConfig(scaled(Span(10000, Millis)))

  lazy val id = UUID.fromString("14c73860-cc5f-40e7-af8f-812e54545cc3")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var aut: ActorRef = null

  override def afterEach(testData: TestData): Unit = {
    system.stop(aut)
  }


  "A SingletonManager" must {

    "retry obtaining lock info if getting lock info fails" in {
      val context = mock[SingletonContext]
      val li = LockInfo("noop1", 2, true, Some(id), 2, None, None, None)
      when(context.getLockInfo(any[String])(any())).thenReturn(Future.failed(new RuntimeException("Test-Execption")))

      aut = SingletonManager[NoOpWorker](li.name, context,
        SingletonManagerConfig(lockEnabled = true, initialMaxDelay = 1.second,
          checkInterval = 2.seconds, lockTtl = 10.seconds))


      eventually {
        verify(context, times(2)).getLockInfo(li.name)(system.dispatcher)
      }
    }

    "initialize lock if lock data is not in database yet" in {
      var cancelled = false
      val context = mock[SingletonContext]
      val li = LockInfo("noop2", 2, true, Some(id), 2, None, None, None)
      val smc = SingletonManagerConfig(lockEnabled = true, initialMaxDelay = 1.second,
        checkInterval = 2.seconds, lockTtl = 10.seconds)
      when(context.getLockInfo(any[String])(any())).thenReturn(Future.successful(None))
      when(context.initLock(any[String], any[FiniteDuration], any[FiniteDuration], any[Boolean])(any())).thenReturn(Future.successful(true))

      aut = SingletonManager[NoOpWorker](li.name, context,smc)


      eventually {
        verify(context, atLeastOnce()).getLockInfo(li.name)(system.dispatcher)
        verify(context, atLeastOnce()).initLock(li.name, smc.checkInterval, smc.lockTtl, li.enabled)(system.dispatcher)
      }
    }

//    "xxxxialize lock if lock data is not in database yet" in {
//      var cancelled = false
//      val context = mock[SingletonContext]
//      val li = LockInfo("noop", 2, true, Some(id), 2, None, None, None)
//      val smc = SingletonManagerConfig(lockEnabled = true, initialMaxDelay = 1.second,
//        checkInterval = 2.seconds, lockTtl = 10.seconds)
//      when(context.getLockInfo(any[String])(any())).thenReturn(Future.successful(Some(li)))
////      when(context.initLock(any[String], any[FiniteDuration], any[FiniteDuration], any[Boolean])(any())).thenReturn(Future.successful(true))
////      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.successful(()))
//      when(context.tryLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.successful(true))
////      when(context.storeState(any[String], any[String])(any())).thenReturn(Future.successful(()))
//
//
//      aut = SingletonManager[NoOpWorker]("noop", context,smc)
//
//
//      eventually {
//        verify(context, atLeastOnce()).getLockInfo(li.name)(system.dispatcher)
////        verify(context,atLeastOnce()).touchLock(li.lockttlsec, li.lock.get, li.name)(system.dispatcher)
//        verify(context,atLeastOnce()).storeState("anystate",li.name)(system.dispatcher)
//
//      }
//    }

    //    "cancel a job if the database update of the lock TTL fails" in {
    //      var cancelled = false
    //      val context = mock[SingletonContext]
    //      val li = LockInfo("test", 10, true, Some(id), 2, None, None, None)
    //      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.failed(new RuntimeException("")))
    //      aut = system.actorOf(LockToucher.props(id, li, context, (s: String, t: Throwable) => cancelled = true))
    //      eventually {
    //        cancelled should be(true)
    //      }
    //    }
    //
    //    "periodically invoke the database query to prolong the lock TTL" in {
    //      val context = mock[SingletonContext]
    //      val li = LockInfo("test", 10, true, Some(id), 4, None, None, None)
    //      when(context.touchLock(any[Int], any[UUID], any[String])(any())).thenReturn(Future.successful(()))
    //      val a:TestActorRef[LockToucher] = TestActorRef(LockToucher.props(id, li, context, (s: String, t: Throwable) => ()))
    //
    //      eventually {
    //        verify(context,times(2)).touchLock(li.lockttlsec, id, li.name)(a.underlyingActor.context.dispatcher)
    //      }
    //
    //    }


//    private[SingletonManager] def shouldTryLock(now: DateTime, lockInfo: LockInfo): Boolean = {
//      if (lockInfo.enabled && lockInfo.lock.isEmpty) {
//        lockInfo.schedule match {
//          case None => true
//          case Some(schedule) =>
//            val last = lockInfo.last.getOrElse(utcNow)
//            val nextRunDate: Date = schedule.getNextValidTimeAfter(last.toDate)
//            // Is work due? (Is next due date in the past?)
//            now.isAfter(nextRunDate.getTime - 1)
//        }
//
//      } else
//        false
//    }


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

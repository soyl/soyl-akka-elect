package io.soyl.elect

import java.util.{TimeZone, UUID}

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import io.soyl.elect.jobs.NoOpWorker
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.quartz.CronExpression
import org.scalatest._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}

import scala.concurrent.Future
import scala.concurrent.duration._

class SingletonManagerObjcetSpec extends WordSpec with PrivateMethodTester with Matchers with MockitoSugar {

  lazy val id = UUID.fromString("14c73860-cc5f-40e7-af8f-812e54545cc3")


  lazy val shouldTryLock = PrivateMethod[Boolean]('shouldTryLock)


  "shouldTryLock" must {
    "indicate not trying lock if lock is disabled" in {
      lazy val li = LockInfo("test", 2, enabled = false, Some(id), 2, schedule = None, last = None, state = None)
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
      val go = SingletonManager invokePrivate shouldTryLock(date, li)
      go should be(false)
    }

    "indicate not trying lock if lock is taken" in {
      lazy val li = LockInfo("test", 2, enabled = true, Some(id), 2, schedule = None, last = None, state = None)
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
      val go = SingletonManager invokePrivate shouldTryLock(date, li)
      go should be(false)
    }

    "indicate try lock if last is None" in {
      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = None, last = None, state = None)
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
      val go = SingletonManager invokePrivate shouldTryLock(date, li)

      go should be(true)

    }
//    "indicate try lock if last is None" in {
//      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = None, last = None, state = None)
//      lazy val date: DateTime = ISODateTimeFormat.dateTime()
//        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
//      val go = SingletonManager invokePrivate shouldTryLock(date, li)
//
//      go should be(true)
//
//    }

    "indicate not try lock if there is no schedule and last is beyond now" in {
      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = None, Some(date.plusHours(1)), state = None)
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
      val go = SingletonManager invokePrivate shouldTryLock(date, li)

      go should be(false)

    }

    "indicate try lock if there is no schedule and last is before now" in {
      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = None, Some(date.minusHours(1)), state = None)
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
      val go = SingletonManager invokePrivate shouldTryLock(date, li)

      go should be(true)

    }

    "indicate try lock if the schedule is 'every day at 10:00', last is yesterday 9:00 and now is 9:00" in {
      lazy val ce = new CronExpression("0 0 10 * * ? *")
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2016-01-30T9:00:00.000Z")
      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = Some(ce), Some(date.minusDays(1)), state = None)
      val go = SingletonManager invokePrivate shouldTryLock(date, li)

      go should be(true)

    }

    "indicate not try lock if the schedule is 'every day at 10:00', last is yesterday 11:00 and now is 9:00" in {
      lazy val ce = new CronExpression("0 0 10 * * ? *")
      ce.setTimeZone(TimeZone.getTimeZone("UTC"))
      lazy val date: DateTime = ISODateTimeFormat.dateTime()
        .withZone(DateTimeZone.UTC).parseDateTime("2016-01-30T9:00:00.000Z")
      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = Some(ce), Some(date.minusHours(22)), state = None)
      val go = SingletonManager invokePrivate shouldTryLock(date, li)

      go should be(false)

    }

//    "bindicate try lock if there is a schedule and next due is in the past" in {
//      lazy val ce = new CronExpression("0 0 10 * * ? *")
//      lazy val date: DateTime = ISODateTimeFormat.dateTime()
//        .withZone(DateTimeZone.UTC).parseDateTime("2016-01-30T11:00:00.000Z")
//      lazy val li = LockInfo("test", 2, enabled = true, None, 2, schedule = Some(ce), Some(date.minusDays(1)), state = None)
//      val go = SingletonManager invokePrivate shouldTryLock(date, li)
//
//      go should be(false)
//
//    }

//    "indicate try lock if " in {
//      lazy val li = LockInfo("test", 2, enabled = true, Some(id), 2, schedule = None, last = None, state = None)
//      lazy val date: DateTime = ISODateTimeFormat.dateTime()
//        .withZone(DateTimeZone.UTC).parseDateTime("2015-07-21T12:00:00.000Z")
//      val go = SingletonManager invokePrivate shouldTryLock(date, li)
//
//      go should be(true)
//
//    }
  }


}

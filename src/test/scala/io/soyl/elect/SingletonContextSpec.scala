package io.soyl.elect


import java.util.UUID

import com.datastax.driver.core.Row
import com.datastax.driver.core.utils.UUIDs
import io.soyl.elect.testutils.CassandraSpec
import org.joda.time.DateTime
import org.quartz.CronExpression
import org.scalatest.concurrent.Eventually._
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FunSpec, Matchers, PrivateMethodTester}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

//class SingletonContextSpec extends FunSpec with Matchers with PrivateMethodTester with MockitoSugar{
class SingletonContextSpec extends CassandraSpec with PrivateMethodTester with Matchers with MockitoSugar {

  lazy val sc = new SingletonContext(getSession())
  //implicit val patienceConfig = new PatienceConfig(scaled(Span(10000, Millis)))
  lazy val id = UUID.fromString("14c73860-cc5f-40e7-af8f-812e54545cc3")


  "A SingletonContext" must {
    "return None for getLockInfo() when lock data not exists" in {
      val f = sc.getLockInfo("test")

      whenReady(f) { maybeLockInfo =>
        maybeLockInfo should be(None)
      }

    }
    "return true for lock initialization" in {
      val li = LockInfo("test", 10, true, Some(id), 4, None, None, None)
      val f = sc.initLock("test", li.checksec.seconds, li.lockttlsec.seconds, true)
      whenReady(f) { maybeApplied =>
        maybeApplied should be(true)
      }

    }

    "return the correct data for getLockInfo() when lock data exists" in {
      val f = sc.getLockInfo("test")
      whenReady(f) { maybeLockInfo =>
        maybeLockInfo.get.name should be("test")
      }

    }
  }

  // Note: The following times need to be summer times (e.g. August), because
  // otherwise the Time zone should be given with +01:00.
  //  lazy val d10am: DateTime = Util.isoFmt.parseDateTime("2015-08-01T10:00:00.000+02:00")
  //  lazy val d10_20am: DateTime = Util.isoFmt.parseDateTime("2015-08-01T10:20:00.000+02:00")
  //  lazy val d09am: DateTime = Util.isoFmt.parseDateTime("2015-08-01T09:00:00.000+02:00")
  //  lazy val d08am: DateTime = Util.isoFmt.parseDateTime("2015-08-01T08:00:00.000+02:00")
  //  lazy val d09_40am: DateTime = Util.isoFmt.parseDateTime("2015-08-01T09:40:00.000+02:00")
  //
  //  lazy val id = UUIDs.random()
  //
  lazy val lockInfoFromRow = PrivateMethod[LockInfo]('lockInfoFromRow)
  //  lazy val everyHour = new CronExpression("0 0 * * * ?")
  //  lazy val everyDay10am = new CronExpression("0 0 10 * * ?")

  //  LockInfo(
  //    row.getString(Table.Name),
  //    row.getInt(Table.Checksec),
  //    row.getBool(Table.Enabled),
  //    Option(row.getUUID(Table.Lock)),
  //    row.getInt(Table.Lockttlsec),
  //    Option(row.getString(Table.Schedule))
  //      .flatMap(s => if (s == "") None else Some(s))
  //      .map(s => new CronExpression(s)),
  //    Option(row.getDate(Table.Last)).map(d => new DateTime(d)),
  //    Option(row.getString(Table.State))
  //  )

  "SingletonDeps#lockInfoFromRow" must {
    "create a LockInfo correctly from a Cassandra row" in {
      val row = mock[Row]
      when(row.getString("name")).thenReturn("streaming")
      when(row.getBool(any[String])).thenReturn(true)
      when(row.getInt(any[String])).thenReturn(10)
      when(row.getUUID("lock")).thenReturn(null)
      when(row.getDate("last")).thenReturn(null)
      when(row.getString("state")).thenReturn(null)
      when(row.getString("schedule")).thenReturn(null)
      val li: LockInfo = SingletonContext invokePrivate lockInfoFromRow(row)
      //val li:LockInfo = SingletonContext.lockInfoFromRow(row)
      li.name should be("streaming")
      li.schedule should be(None)
    }

    //    it("2") {
    //      val row = mock[Row]
    //      when(row.getString("name")).thenReturn("streaming")
    //      when(row.getBool(any[String])).thenReturn(true)
    //      when(row.getInt(any[String])).thenReturn(10)
    //      when(row.getUUID("lock")).thenReturn(null)
    //      when(row.getDate("last")).thenReturn(null)
    //      when(row.getString("state")).thenReturn(null)
    //      when(row.getString("schedule")).thenReturn("")
    //      //val li:LockInfo = SingletonDeps invokePrivate lockInfoFromRow(row)
    //      val li:LockInfo = SingletonContext.lockInfoFromRow(row)
    //      li.name should be("streaming")
    //      li.schedule should be(None)
    //    }
  }
}


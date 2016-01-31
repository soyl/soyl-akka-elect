package io.soyl.elect

import java.util.{TimeZone, UUID}

import akka.actor.ActorSystem
import com.datastax.driver.core.{Row, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import org.joda.time.DateTime
import org.quartz.CronExpression

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Handle class to provide context used by all singletons (mostly the Cassandra
  * database connection for now)
  *
  * @param session The [[Session]] to use for statement preparing and execution
  */
class SingletonContext(session: => Session) {


  /**
    * Touch the lock with the provided TTL. This is used by the SingletonManager to
    * keep a lock locked as long as the worker is working.
    *
    * @param ttlSecs
    * @param id
    * @param name
    * @param ec
    * @return
    */
  def touchLock(ttlSecs: Int, id: UUID, name: String)(implicit ec: ExecutionContext): Future[Unit] = {
    import CassandraUtils._
    session.executeAsync(touchLockStmt.bind(new Integer(ttlSecs), id, name))
      .map(resultSet => ())
  }

  private lazy val touchLockStmt = session.prepare(QueryBuilder
    .update(Table.TableName)
    .`with`(QueryBuilder.set(Table.Lock, QueryBuilder.bindMarker()))
    .where(QueryBuilder.eq(Table.Name, QueryBuilder.bindMarker()))
    .using(QueryBuilder.ttl(QueryBuilder.bindMarker())))


  /**
    * Try to acquire a specific lock. This uses Cassandra CAS feature to ensure that only
    * one singleton manager can grab the lock.
    *
    * Uses cross-data centre serial consistency for now.
    *
    * (See https://github.com/soyl/soyl-akka-elect/issues/1)
    *
    * @param ttlSecs
    * @param id
    * @param name
    * @param ec
    * @return
    */
  def tryLock(ttlSecs: Int, id: UUID, name: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    import CassandraUtils._

    session.executeAsync(tryLockStmt.bind(new Integer(ttlSecs), id, name))
      .map(resultSet => {
        Option(resultSet.one) match {
          case None => throw new RuntimeException(s"Workspace row not found, please add it manually FIXME: doclink")
          case Some(row) => row.getBool(0)
        }
      })
  }

  private lazy val tryLockStmt = session.prepare(QueryBuilder
    .update(Table.TableName)
    .`with`(QueryBuilder.set(Table.Lock, QueryBuilder.bindMarker()))
    .where(QueryBuilder.eq(Table.Name, QueryBuilder.bindMarker()))
    .onlyIf(QueryBuilder.eq(Table.Lock, null)).using(QueryBuilder.ttl(QueryBuilder.bindMarker())))


  /**
    * Obtain the meta data of a given lock.
    *
    * @param name
    * @param ec
    * @return [[None]] if the lock is not yet present with meta data, Some(lockInfo)
    *         otherwise.
    */
  def getLockInfo(name: String)(implicit ec: ExecutionContext): Future[Option[LockInfo]] = {
    import CassandraUtils._

    session.executeAsync(getLockInfoStmt.bind(name))
      .map(resultSet => {
        Option(resultSet.one).map(SingletonContext.lockInfoFromRow)
      })
  }

  private lazy val getLockInfoStmt = session.prepare(QueryBuilder
    .select()
    .column(Table.Name)
    .column(Table.Checksec)
    .column(Table.Enabled)
    .column(Table.Lock)
    .column(Table.Lockttlsec)
    .column(Table.Schedule)
    .column(Table.Last)
    .column(Table.State)
    .from(Table.TableName)
    .where(QueryBuilder.eq(Table.Name, QueryBuilder.bindMarker())))

  /**
    * Commit a new state to be associated with the lock - workers use this
    * state information to coordinate the work progress across invokations.
    *
    * The date of the last run is not updated by this call because a commit
    * is just intended to reduce re-processing in case of failure.
    *
    * @param state
    * @param name
    * @param ec
    * @return
    */
  def commitState(state: String, name: String)(implicit ec: ExecutionContext): Future[Unit] = {
    import CassandraUtils._
    session.executeAsync(commitStateStmt.bind(state, name))
      .map(resultSet => ())
  }

  private lazy val commitStateStmt = session.prepare(QueryBuilder
    .update(Table.TableName)
    .`with`(QueryBuilder.set(Table.State, QueryBuilder.bindMarker()))
    .where(QueryBuilder.eq(Table.Name, QueryBuilder.bindMarker())))

  /**
    * Store a new state to be associated with the lock - workers use this
    * state information to coordinate the work progress across invokations.
    *
    * @param state
    * @param name
    * @param ec
    * @return
    */
  def storeState(state: String, name: String)(implicit ec: ExecutionContext): Future[Unit] = {
    import CassandraUtils._
    session.executeAsync(storeStateStmt.bind(state, name))
      .map(resultSet => ())
  }

  private lazy val storeStateStmt = session.prepare(QueryBuilder
    .update(Table.TableName)
    .`with`(QueryBuilder.set(Table.State, QueryBuilder.bindMarker()))
    .and(QueryBuilder.set(Table.Last,
      QueryBuilder.fcall("dateOf", QueryBuilder.fcall("now"))))
    .where(QueryBuilder.eq(Table.Name, QueryBuilder.bindMarker())))

  /**
    * Initialize a lock entry with default / provided lock meta data.
    * This is used by the first singleton manager that discovers that
    * a given lock is not yet present.
    *
    * Uses Cassandra CAS functionality to ensure only one manager is
    * initializing the lock.
    *
    * @param name
    * @param checkInterval
    * @param ttl
    * @param enabled
    * @param ec
    * @return
    */
  def initLock(name: String, checkInterval: FiniteDuration = 20.seconds,
               ttl: FiniteDuration = 240.seconds, enabled: Boolean = false)(implicit ec: ExecutionContext): Future[Boolean] = {
    import CassandraUtils._
    val jEnabled: java.lang.Boolean = enabled // Make sure this happens the way I want
    session.executeAsync(initLockStmt
      .bind(name, new Integer(checkInterval.toSeconds.toInt),
        jEnabled, new Integer(ttl.toSeconds.toInt)
      ))
      .map(resultSet => {
        Option(resultSet.one) match {
          case None => throw new RuntimeException(s"Unable to initialize missing lock row: CAS query did not return any row")
          case Some(row) => row.getBool(0)
        }
      })
  }

  private lazy val initLockStmt = session.prepare(QueryBuilder
    .insertInto(Table.TableName)
    .value(Table.Name, QueryBuilder.bindMarker())
    .value(Table.Checksec, QueryBuilder.bindMarker())
    .value(Table.Enabled, QueryBuilder.bindMarker())
    .value(Table.Lockttlsec, QueryBuilder.bindMarker())
    .ifNotExists())

}

object SingletonContext {

  /**
    * Create [[LockInfo]] from Cassandra [[Row]].
    *
    * @param row
    * @return
    */
  private[SingletonContext] def lockInfoFromRow(row: Row): LockInfo = {
    LockInfo(
      row.getString(Table.Name),
      row.getInt(Table.Checksec),
      row.getBool(Table.Enabled),
      Option(row.getUUID(Table.Lock)),
      row.getInt(Table.Lockttlsec),
      Option(row.getString(Table.Schedule))
        .flatMap(s => if (s == "") None else Some(s))
        .map(s => utcCronExpression(s)),
      Option(row.getDate(Table.Last)).map(d => new DateTime(d)),
      Option(row.getString(Table.State))
    )
  }

  private def utcCronExpression(s: String): CronExpression = {
    val ce = new CronExpression(s)
    ce.setTimeZone(TimeZone.getTimeZone("UTC"))
    ce
  }

}

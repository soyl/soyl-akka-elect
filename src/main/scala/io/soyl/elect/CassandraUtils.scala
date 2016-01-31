package io.soyl.elect

import com.datastax.driver.core.{ResultSet, ResultSetFuture}
import com.google.common.util.concurrent.{FutureCallback, Futures}

import scala.concurrent.{Promise, Future}

object CassandraUtils {

  /**
    * Evidence to turn java-driver result set future into Scala future.
    * @param resultSet
    * @return
    */
  implicit def toScalaFuture(resultSet: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback[ResultSet](resultSet, new FutureCallback[ResultSet] {
      override def onSuccess(result: ResultSet): Unit = p success result

      override def onFailure(t: Throwable): Unit = p failure t
    })
    p.future
  }

}

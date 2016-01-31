package io.soyl.elect.testutils


import ch.qos.logback.classic.{Logger, Level}
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.scalalogging.LazyLogging
import org.apache.cassandra.service.CassandraDaemon
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, FunSpec}

import scala.util.Try

abstract class CassandraSpec extends WordSpecLike with BeforeAndAfterAll with ScalaFutures with IntegrationPatience
{

  private lazy val (cassandra, cluster, session) = init()
  val keyspace = "electtest"

  private def init(): (CassandraDaemon, Cluster, Session) = {
    val _cassandra = new CassandraDaemon()
    val _config = this.getClass.getResource("/cu-cassandra.yaml").toString
    sys.props += "cassandra.config" -> _config
    sys.props += "cassandra-foreground" -> "true"
    _cassandra.activate()

    val _cluster = Cluster.builder().addContactPoint("127.0.0.1").withPort(9142).build()
    val initSession = _cluster.connect()
    initSession.execute(s"create keyspace if not exists $keyspace with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }")
    initSession.close()

    val _session = _cluster.connect(keyspace)

    _session.execute(
      """
        |create table if not exists locks (
        |      name text,
        |      enabled boolean,
        |      lockttlsec int,
        |      checksec int,
        |      schedule text,
        |      last timestamp,
        |      lock uuid,
        |      state text,
        |      primary key (name)
        |);
        |
      """.stripMargin)


    (_cassandra, _cluster, _session)

  }

  override protected def afterAll(): Unit = {
    Try(session.execute("drop keyspace %s".format(keyspace)))
    session.close()
    cluster.close()
    cassandra.deactivate()
  }

  protected def getSession(): Session = {
    session
  }

}


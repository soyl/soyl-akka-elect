package io.soyl.elect

//import akka.actor.ActorSystem
//import com.datastax.driver.core.{Session, Cluster}
//import com.typesafe.scalalogging.LazyLogging
//import io.soyl.elect.jobs.{B, A}
//import io.soyl.elect.testutils.CassandraSpec
//import org.apache.cassandra.service.CassandraDaemon
//
//import scala.concurrent.duration._
//
//
//class ATest extends CassandraSpec with LazyLogging {
//
// implicit lazy val xx = ActorSystem("MySpec")
//
//  "LockRepository" must {
//    "save and delete lock entries" in {
//
//      //val r = getSession().execute("select * from locks")
////      println("============> " + r.all())
//
//      val e = new SingletonContext(getSession())
//
//      val s = SingletonManager[A]("foobar",e,SingletonManagerConfig(lockEnabled = true,initialMaxDelay = 1.second))
//
//      val s2 = SingletonManager[B]( new B(10),"foobar",e,SingletonManagerConfig(lockEnabled = true,initialMaxDelay = 1.second))
//
//
//      Thread.sleep(100000)
//      val v = getSession().execute("select * from locks")
//      logger.info("============> " + v.one())
//      Thread.sleep(100000)
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//      logger.info("============================================================================> ")
//
//      2 should be(2)
//
//    }
//  }
//
//
//}

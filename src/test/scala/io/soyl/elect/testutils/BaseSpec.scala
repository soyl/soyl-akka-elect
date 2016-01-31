package io.soyl.elect.testutils

import com.typesafe.scalalogging.LazyLogging
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, Eventually}
import org.scalatest.mock.MockitoSugar

abstract class BaseSpec extends WordSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers
with OptionValues with EitherValues with MockitoSugar
with Eventually with IntegrationPatience with LazyLogging {

}

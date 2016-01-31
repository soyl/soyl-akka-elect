package io.soyl.elect

import java.util.UUID

import org.joda.time.DateTime
import org.quartz.CronExpression

/**
 * This class represents the information stored in the 'locks' table.
 * See also [[Table]].
 *
 * @param name
 * @param checksec
 * @param enabled
 * @param lock
 * @param lockttlsec
 * @param schedule
 * @param last
 * @param state
 */
case class LockInfo(name:String, checksec:Int, enabled:Boolean, lock:Option[UUID],
                           lockttlsec:Int,schedule:Option[CronExpression],last:Option[DateTime],state:Option[String]) {

}

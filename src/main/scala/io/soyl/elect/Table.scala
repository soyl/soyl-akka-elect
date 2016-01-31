package io.soyl.elect

object Table {
  val TableName = "locks"
  /** Name of the lock */
  val Name = "name"
  /** Actors competing for this lock should try to acquire it every checksec seconds */
  val Checksec = "checksec"
  /** If disabled, competitors should never execute work associated with this lock */
  val Enabled = "enabled"
  /** The UUID of the competitor that curently holds the lock */
  val Lock = "lock"
  /** The TTL of the lock in seconds */
  val Lockttlsec = "lockttlsec"
  /** An optional cron expression if work should only be executed at certain points in time */
  val Schedule = "schedule"
  /** Date and time in UTC of the last successful run */
  val Last = "last"
  /** A generic current state associated with the work coordinated by this lock */
  val State = "state"
}



package io.soyl.elect

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
  * Configuration for leaders beyond the configuration provided by the log itself.
  *
  * @param initialMaxDelay The manager will pick a random startup delay between
  *                        0 and initialMaxDelay in order to avoid that all managers
  *                        try to grab a lock at the same time upon startup.
  * @param checkInterval Specifies how often the [[SingletonManager]] will try to
  *                      acquire the given lock in the absence of retrieved [[LockInfo]]
  *                      and to which check interval the lock will be set if tt is
  *                      initially created by a [[SingletonManager]]
  * @param lockTtl Specifies the TTL of the lock if it is initially created by a [[SingletonManager]]
  * @param lockEnabled Specifies whether the lock is initially created as active or inactive.
  */
case class SingletonManagerConfig(
                                   initialMaxDelay: FiniteDuration = 10.seconds,
                                   checkInterval: FiniteDuration = 20.seconds,
                                   lockTtl: FiniteDuration = 240.seconds,
                                   lockEnabled: Boolean = false
                                 )

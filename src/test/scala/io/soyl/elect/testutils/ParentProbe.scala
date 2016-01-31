package io.soyl.elect.testutils

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestProbe

object ParentProbe {
  def apply(props: Props)(implicit system: ActorSystem): ParentProbe = {
    new ParentProbe(props,system)
  }
}

class ParentProbe(props:Props,_system:ActorSystem) extends TestProbe(_system) {

  val parent = system.actorOf(Props(new Actor {
    val child = context.actorOf(props, "child")

    def receive = {
      case x if sender == child => ref forward x
      case x => child forward x
    }
  }))

  def sendViaParent(msg:Any):Unit = {
    send(parent,msg)
  }
}



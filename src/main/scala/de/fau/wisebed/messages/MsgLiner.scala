package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection
import eu.wisebed.api.common.Message

trait MsgLiner extends MessageInput {
	private var mbuf = Map[String, Message]()

	abstract override def handleMsg(msg: Message) {
		//Check whether we have some old data
		val wmsg = mbuf.get(msg.node) match {
			case Some(old) => {
				//Remove from map
				mbuf -= msg.node
				//Add new data to old data
				old.data ++= msg.data
				//Update timestamp
				old.timestamp = msg.timestamp
				old
			}
			//If there is no old mes just us it
			case _ => msg
		}
		
		// Get data
		val str = wmsg.dataString
		// Split at CR
		val split = str.split("\n")
		
		split.foreach(x => {
			val snd = wmsg.copy
			snd.dataString = x
			if(x != split.last || str.last == '\n'){
				super.handleMsg(snd)
			} else {
				mbuf += wmsg.node -> snd
			}
		})
	}
}

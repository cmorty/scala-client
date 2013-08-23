package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.WisebedApiConversions._
import scala.collection
import eu.wisebed.api.common.Message
import scala.collection.mutable.ArrayBuffer

trait MsgLiner extends MessageInput {
	private var mbuf = Map[String, ArrayBuffer[Byte]]()

	abstract override def handleMsg(msg: Message) {
		
		var outp =  mbuf.getOrElse(msg.node, ArrayBuffer[Byte]())
		
		for(e <- msg.data){
			if(e == '\n'){
				val snd = msg.copy
				snd.data = outp.toArray
				outp = ArrayBuffer[Byte]()
				super.handleMsg(snd)
			} else {
				outp += e
			}
		}

		mbuf += msg.node -> outp

	}
}

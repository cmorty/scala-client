package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.RemMes
import de.fau.wisebed.StopAct
import scala.actors.Actor
import org.slf4j.Logger
import scala.ref.WeakReference







abstract trait MessageInput extends Actor {
	protected val log:Logger
	private var state  = 0
	protected val cbExit = scala.collection.mutable.ArrayBuffer[() => Unit]()
	
	protected def handleMsg(msg:common.Message):Unit
	protected def stopIntput { if(state == 0) state = 1 }
	
	def isWeak = false
	
	//This is for debugging
	private val id = MessageInput.getId
	
	/**
	 * Wait for the MessageInput to handle its last message 
	 */
	def waitDone(){
		while(state != 3) wait
	} 
	
	/**
	 * Run ager the last message -> e.g. close file handle.
	 */
	def runOnExit(cb: () => Unit){
		cbExit += cb
	}
	
	
	override def exit(): Nothing  = {		
		cbExit.foreach(_())
		super.exit
	}
	
	
	def act() {
		log.debug("Message actor {} started: {}", id.toString, this.toString)

		loop {
			react {				
				case s:common.Message =>
					log.trace("MA {} got {}", id.toString, s.hashCode)
					if(state == 0) handleMsg(s)
					if(state == 1){
						sender ! RemMes(this)
						state = 2
					} 
				case StopAct =>
					log.debug("Message actor {} stopped: {}", id.toString, this.toString)
					state = 3
					synchronized{notify}
					exit
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}
	}
}


object MessageInput {
	var ctr = 0
	def getId = {ctr += 1 ; ctr -1}
}
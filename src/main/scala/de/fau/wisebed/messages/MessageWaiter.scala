package de.fau.wisebed.messages

import scala.concurrent.Future
import scala.concurrent.Promise
import eu.wisebed.api._
import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection.mutable
import org.slf4j.LoggerFactory
import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.CanAwait
import scala.util.Try
import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeoutException



/**
 * @param nodes The nodes to monitor
 * @param needle The string to wait for. This may be multiline 
 *
 */
class MessageWaiter(nodes:Iterable[String], needle:String) extends MessageInput with Future[Boolean]{
	
	val prom = Promise[Boolean]()
	
	val log = LoggerFactory.getLogger(this.getClass) 
	private var stop = false
	
	
	private var mbuf = nodes.map(_ -> "").toMap
	private var ready = nodes.map(_ -> false).toMap
	
	
	def isDone:Boolean = prom.isCompleted	
	def isOK:Boolean = ready.forall(_._2 == true)
	
	override def isWeak = true
	
	def apply():Boolean = Await.result(prom.future, Duration.Inf)
	
	def waitTimeout(timeout:Int):Boolean = synchronized{
		def date = (new Date).getTime
		var ctime:Long = date
		val time:Long = ctime + timeout
		while( {ctime = date; ctime} < time && !isDone) wait(time - ctime)
		isOK
	}
	
	def unregister = synchronized {
		stop = true
		notify
		stopIntput
	}
	
	
	override def handleMsg(msg:common.Message) {
		val stro = mbuf.get(msg.node)
		if(stro.isEmpty) return		
		val str = stro.get + msg.dataString		
		if(str.contains(needle)){
			ready += (msg.node -> true)
			mbuf -= msg.node
			if(isOK) unregister
			
		} else {
			// Save the rest of the String, that might contain the needle. More optimization is unreasonable. 
			mbuf += (msg.node -> str.takeRight(needle.length) ) 
		}		
	}
	
	def result(atMost: Duration)(implicit permit: CanAwait) = prom.future.result(atMost)
	def value = prom.future.value
	def onComplete[U](func: (Try[Boolean]) ⇒ U)(implicit executor: ExecutionContext) = prom.future.onComplete(func)
	def isCompleted = prom.future.isCompleted
	
	def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
	 	if(prom.future.ready(atMost).isCompleted) this
	 	else throw new TimeoutException("Futures timed out after [" + atMost + "]")
	}
}


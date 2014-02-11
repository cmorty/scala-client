package de.fau.wisebed.messages

import scala.concurrent.Future
import scala.concurrent.Promise
import eu.wisebed.api._
import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.WisebedApiConversions._
import scala.collection.mutable
import org.slf4j.LoggerFactory
import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.CanAwait
import scala.util.Try
import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeoutException
import scala.util.Success
import scala.util.Failure




/**
 * Waits for all Node the send a certain Message
 * @param nodes The nodes to monitor
 * @param needle The string to wait for. This may be multi line 
 */
class MessageWaiter(nodes:Iterable[String], needle:String) extends MessageInput with Future[Map[String, Boolean]]{
	
	val log = LoggerFactory.getLogger(this.getClass)
	
	private val prom = Promise[Map[String, Boolean]]()
	
	
	private var stop = false
	
	private var mbuf = nodes.map(_ -> "").toMap
	
	private var ready = nodes.map(_ -> false).toMap
	
	
	
	
	def isDone:Boolean = prom.isCompleted	
	def isOK:Boolean = ready.forall(_._2 == true)
	
	override def isWeak = true
	
	
	/**
	 * @param atMost The maximum time to wait
	 * @return True on success, false if times out.
	 */
	def result(atMost: Duration)(implicit permit: CanAwait) = prom.future.result(atMost)
	
	def waitResult(atMost: Duration):Boolean = {
		try {
			Await.result(prom.future, atMost)
			isOK
		} catch {
			case e: TimeoutException => false
			case e: Exception => throw e
		}
	}
	
	def successMap = ready
	
	
	/**
	 * @param time The maximum time to wait in milliseconds
	 * @return True on success, false if times out.
	 */
	def waitResult(milliseconds: Long):Boolean = waitResult(Duration(milliseconds, MILLISECONDS))
	
	def apply():Boolean = waitResult(Duration.Inf)
	
	
	def unregister = synchronized {
		prom.complete({if(isOK) new Success(ready) else new Failure(new Exception("Stopped before finished"))})
		log.info("Finished promise! with: " + isOK)
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
	
	
	def value:Option[Try[Map[String, Boolean]]] =  prom.future.value
	def onComplete[U](func: (Try[Map[String, Boolean]]) ⇒ U)(implicit executor: ExecutionContext) = prom.future.onComplete(func)
	def isCompleted = prom.future.isCompleted
	
	def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
	 	if(prom.future.ready(atMost).isCompleted) this
	 	else throw new TimeoutException("Futures timed out after [" + atMost + "]")
	}
}


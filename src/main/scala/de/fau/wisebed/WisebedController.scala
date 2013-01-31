package de.fau.wisebed

import java.lang.Override
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.concurrent.Executors

import scala.actors.DaemonActor
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListMap
import scala.collection.mutable.SynchronizedSet
import scala.ref.WeakReference

import org.slf4j.LoggerFactory

import de.fau.wisebed.jobs.Job
import de.fau.wisebed.messages.MessageInput
import eu.wisebed.api.common.Message
import eu.wisebed.api.controller.Controller
import eu.wisebed.api.controller.RequestStatus
import javax.jws.WebParam
import javax.jws.WebService
import javax.xml.ws.Endpoint


case object StopAct

protected case object ReqJob
protected case class AddJob[S](id:String, job:Job[S])	
case class RemJob[S](job:Job[S])

protected case class AddMes(mi:messages.MessageInput)
protected case class RemMes(mi:messages.MessageInput)
protected case class RemMesAll()

private class MessageInputHolder(mi:MessageInput){
	val wr:WeakReference[MessageInput] = if(mi.isWeak)
			new WeakReference(mi)
		 else 
			null
			
	val r:MessageInput = if(mi.isWeak) null else mi
	def get():Option[MessageInput] = if(r != null) Some(r) else wr.get
	
	
	def weak = r == null
	
	
	override def equals(o:Any) = o match {
		case x:MessageInputHolder => 
			
			if(!weak &&  !x.weak) this.r == x.r //Both non-wark
			else if(weak && x.weak) {
				val t = wr.get
				val o = x.wr.get		
				// If both are none or both are not none and equal
				(t == None && o == None) || (t != None && o != None && t == o)								
			} else 
				false		
		case _ => false
	}
	
	override def hashCode: Int = {
		if(weak){ 
			val t = wr.get
			if(t == None) 0  else t.hashCode 
		} else {
			r.hashCode
		} 
		
	}
	
}



@WebService(
		serviceName = "ControllerService",
		targetNamespace = "urn:ControllerService",
		portName = "ControllerPort",
		endpointInterface = "eu.wisebed.api.controller.Controller"
)
class WisebedController extends Controller {
	
	
  	val log = LoggerFactory.getLogger(WisebedController.this.getClass)

  	val controllerid =  WisebedController.getUniqueID
  	
  	val url = "http://" + InetAddress.getLocalHost.getCanonicalHostName + ":" + WisebedController.port + "/controller/" + controllerid

  	
  
	log.debug("Starting ExperimentController...")
	log.debug("Endpoint URL: {}", url)

	
	
	val endpoints =  WisebedController.interfaces.flatMap(x => {
  	
		   
  			val url = "http://" + x + ":" + WisebedController.port + "/controller/" + controllerid
  			log.debug("Connecting to " + url)
  			try{
  				val ep = Endpoint.publish(url, WisebedController.this)
  				ep.setExecutor(Executors.newCachedThreadPool)
  				List(ep)
  			} catch {
  				case e:Exception =>
  					log.error("Failed to open " + url)
  					List[Endpoint]() 
  			}
  			
  		})
  		
  		
  	
	

	log.debug("Successfully started ExperimentControllers at " + url + " using ip(s): " + WisebedController.interfaces.mkString(", "))
	
	private val messageHandlers = new HashSet[MessageInputHolder] with SynchronizedSet[MessageInputHolder]
	var notificationCallbacks = List[String => Unit]()
	var endCallbacks = List[() => Unit]()
	
	
	/*
	 * This should be a DeamonActor, as the must be someone Non-Deamon to receive or send messages.
	 */
	val sDisp = new DaemonActor{
  		private var rjob = 0
  		private var rsBuf = List[RequestStatus]()
		private val jobs =  new ListMap[String, Job[_]]
  		
  		private def sendJob(rs:RequestStatus){
  			jobs.get(rs.getRequestId) match {
				case x:Some[Job[_]] => rs.getStatus.foreach(s => {x.get ! s ; log.trace("Dispatching {}", rs.getRequestId) }) //Send to Job
				case _ => { 
					if(rjob > 0){
						rsBuf ::= rs
					} else	{
						log.error("Got Job id {} without Job", rs.getRequestId)
					}
				}	
			}  				
  		}
  		
  		
  		def act () {
  			log.debug("Actor Started")
  			/** @todo terminate? */
  			loopWhile(true){
  				react {
  				//*********** Jobs
  					case s:RequestStatus => sendJob(s)
  					case ReqJob => rjob+=1
  					case AddJob(s,j) =>
  						log.debug("Adding job {}", s)
  						jobs += s->j
  						j.start
  						//Remove job from outstanding jobs
  						rjob -= 1
  						//Create new Buffer
  						val buf = rsBuf
  						rsBuf = List[RequestStatus]()
  						//If there are other outstanding jobs, the message will be enqueued again
  						buf.foreach(sendJob(_))
  					case RemJob(j) =>
  						val foo = jobs.find(_._2 == j)
  						jobs.find(_._2 == j) match {
  							case Some(kv) =>
  								log.debug("Removing job {}.", kv._1)
  								jobs.remove(kv._1)
  								kv._2 ! StopAct
  							case None => log.error("RemJob: Job {} not found.", j)
  						}
  				//********* Messages
  					case AddMes(mi) => messageHandlers += new MessageInputHolder(mi); mi.start
  					case RemMes(mi) => 	
  						val mih = new MessageInputHolder(mi)
  						if(!messageHandlers.contains(mih)){
  							log.error("Failed to remove MessageInput: {}", mi)
  						} else {
  							messageHandlers -=  mih
  							mi ! StopAct
  						}
  					case m:Message =>	
  						for(mih <- messageHandlers){
  							val mi = mih.get
  							if(mi == None){
  								log.debug("Removing holder: {}", mih.toString)
  								messageHandlers -= mih
  							} else {
  								mi.get ! m
  							}
  						} 
  					case RemMesAll =>
  						for(mih <- messageHandlers){
  							messageHandlers -=  mih
  							val mi = mih.get  							
  							if(mi != None){
  								mi.get ! StopAct
  							}
  						}
  				//********* Catch potentioal Problems
  					case x => log.error("Got unknow class: {}", x.getClass.toString)
  				
  					
  				}
  			}
  		}
  	}
	
  	sDisp.start
	
	@Override
	def receive(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[Message]) {
  		//Send to dispetcher
		for(m <- msg) sDisp ! m
	}

  	/**
  	 * Add a new message input that will receive all Messages
  	 * One must nor register an input twice.
  	 */
	def addMessageInput(mi:messages.MessageInput){ sDisp ! AddMes(mi) }

	/**
	 * Remove the message input. This will also end it's actor and call the shutdown functions.
	 */
	def remMessageInput(mi:messages.MessageInput){ sDisp ! RemMes(mi) }

	@Override
	def receiveStatus(@WebParam(name = "status", targetNamespace = "") status:java.util.List[RequestStatus]) {
		//Send to dispetcher	
		status.foreach( rs => {
			log.debug("Got Message for {} - sending to Dispatcher", rs.getRequestId)
			sDisp ! rs	
		})
	}

	/**
	 * Add a new Job to the controller.
	 * @param job The Job
	 * @param rid Function to request the Job-ID
	 */
	def addJob[S](job:Job[S], rid: => String) {
		//Send JRequest
		sDisp ! ReqJob
		//Get Job
		val id:String = rid		
		sDisp ! AddJob(id, job)
	}

	@Override
	def receiveNotification(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[String]) {
		for(cb <- notificationCallbacks; s <- msg) cb(s)
	}

	def onNotification(callback: String => Unit) {
		notificationCallbacks.synchronized{
			notificationCallbacks ::= callback
		}
	}

	@Override
	def experimentEnded() {
		for(cb <- endCallbacks) cb()
		sDisp ! RemMesAll
		endpoints.foreach(_.stop)
	}

	def onEnd(callback: => Unit) {
		endCallbacks ::= ( () => callback )
	}
}


object WisebedController{
	private var intid = 1

	private def getUniqueID:String = {(intid +=1); intid.toString}

	private var interfaces = {
		val ifs = enumerationAsScalaIterator(NetworkInterface.getNetworkInterfaces)
		val addr = ifs.filter(x => {x.isUp && !x.isVirtual}).flatMap(_.getInetAddresses).toList
		addr.filter(x => {!x.isLinkLocalAddress}).map( x => {
			if(x.isInstanceOf[java.net.Inet6Address])	"[" + x.getHostAddress.takeWhile(_ != '%') + "]"
			else x.getHostAddress
		})
	}
	
	private lazy val threadPool = Executors.newCachedThreadPool()

	private lazy val port:Int = {
		val socket = new ServerSocket(0)
		val port = socket.getLocalPort()
		socket.close()
		port
	}
}

package de.fau.wisebed

import java.io.InputStream
import java.util.GregorianCalendar
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.Map
import scala.collection.Seq
import scala.collection.mutable.Buffer
import org.slf4j.LoggerFactory
import de.fau.wisebed.WisebedApiConversions._
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.messages.MsgLiner
import eu.wisebed.api.WisebedServiceHelper
import eu.wisebed.api.common
import eu.wisebed.api.wsn
import jobs.FlashJob
import jobs.NodeOkFailJob
import jobs.NodesAliveJob
import wrappers.ChannelHandlerDescription
import wrappers.Program
import de.fau.wisebed.wrappers.ChannelHandlerConfiguration
import de.fau.wisebed.wrappers.Program

class NodeMessage(val node:String, val data:Array[Byte], val time:GregorianCalendar) {
	def this (m:common.Message){
		this(m.getSourceNodeId, m.getBinaryData, m.getTimestamp.toGregorianCalendar)
	}

	def dataString = data.map(_.toChar).mkString
}

case class ExperimentEndedException(t:String) extends Exception(t)

class Experiment (res:List[Reservation], implicit val tb:Testbed) {
	protected val log = LoggerFactory.getLogger(this.getClass)

	var active = true
	def active_ (x:Boolean) {} //Null Setter

	protected val controller = new WisebedController

	if(log.isTraceEnabled){
		val msghndl = new MessageLogger(mi => {
			log.debug("Got message from " + mi.node + ": " + mi.dataString)
		}) with MsgLiner
		controller.addMessageInput(msghndl)
	}
	controller.onEnd {
		active = false
		log.info("Experiment ended")
	}

	
	log.debug("Local controller published on url: {}", controller.url)
	
	protected val wsnService:wsn.WSN = {	

		
		val wsnEndpointURL = tb.sessionManagement.getInstance(res, controller.url)
		log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL)
		WisebedServiceHelper.getWSNService(wsnEndpointURL)
	}
	
	//----------------------- End constructor ---------------------------------------------
	
	//----------------------- Jobs --------------------------------------------------------
	
	private def requireActive = if(!active) throw new ExperimentEndedException("Can not flash nodes, as the experiment ended")
	
	def flash(prog:Program, nodes:Seq[String]):FlashJob = {
		requireActive
		val map = List.fill(nodes.size){new java.lang.Integer(0)}
		val job = new FlashJob(nodes)
		controller.addJob(job, wsnService.flashPrograms(nodes, map, List(wprogram2program(prog))))
		job	
	}
	
	
	def flash(is:InputStream, nodes:Seq[String]):FlashJob = flash(Program(is), nodes)
	
	def flash(file:String, nodes:Seq[String]):FlashJob = flash(Program(file), nodes)
	
	/**
	 * @param nodeProgMap A map of (nodes -> firmware files)
	 * @return The job flashing the nodes
	 */
	def flash(nodeProgMap:Map[String, String]):FlashJob ={
		requireActive
		val nodeList = Buffer[String]()
		val progList = Buffer[wsn.Program]()
		val progMap = Buffer[Integer]()
		val progs = scala.collection.mutable.Map[String, Int]()
		
		
		for((node, file) <- nodeProgMap){
			nodeList += node
			progMap += progs.getOrElseUpdate(file, {
				progList += wprogram2program(Program(file))
				progList.size - 1
			})
			
		}
		val job = new FlashJob(nodeList)		 
		controller.addJob(job, wsnService.flashPrograms(nodeList, progMap, progList))
		job	
	}
	
	/**
	 * @param progNodeMap A Map of programs mapping to a list of nodes 
	 * @return The job flashing the nodes
	 * Using a dummy implicit -> http://stackoverflow.com/questions/3307427
	 */
	def flash(progNodeMap:Map[Program, Seq[String]])(implicit d: DummyImplicit):FlashJob ={
		requireActive
		val nodeList = Buffer[String]()
		val progMap = Buffer[Integer]()
		val progList = Buffer[wsn.Program]()
		
		for((prog, nodes) <- progNodeMap){
			progList += prog
			nodeList ++= nodes
			progMap ++= List.fill(nodes.size){progList.size - 1}
		}
		val job = new FlashJob(nodeList)
		
		/*
		log.debug("Proglist: " + progList.map(_.name).mkString(",") + "\n"+
				nodeList.zip(progMap).map(x => {x._1 + " -> " + x._2}).mkString("\n")
		)*/
		
		controller.addJob(job, wsnService.flashPrograms(nodeList, progMap, progList))
		job	
	}
	
	
	def areNodesAlive(nodes:Seq[String]):NodesAliveJob = {
		requireActive
		val job = new NodesAliveJob(nodes)
		controller.addJob(job, wsnService.areNodesAlive(nodes))
		job
	}
	
	def resetNodes(nodes:Seq[String]):NodeOkFailJob = {
		requireActive
		val job = new NodeOkFailJob("reset", nodes)
		controller.addJob(job, wsnService.resetNodes(nodes))
		job
	}
	
	def send(node:String,data:String):NodeOkFailJob = send(List(node), data)
	
	def send(nodes:Seq[String], data:String):NodeOkFailJob = {
		requireActive
		val job = new NodeOkFailJob("send" , nodes)
		val msg = new common.Message
		msg.setBinaryData(data.toArray.map(_.toByte))
		msg.setSourceNodeId("urn:fauAPI:none:0xFFFF")
		msg.setTimestamp(new GregorianCalendar)
		controller.addJob(job, wsnService.send(nodes, msg))
		job
	}
	
	def setChannelHandler(nodes:Seq[String], cnf:ChannelHandlerConfiguration):NodeOkFailJob = {
		requireActive
		val cn = List.fill(nodes.size){cnf}
		val job =  new NodeOkFailJob("setChannelHandler", nodes)
		controller.addJob(job,wsnService.setChannelPipeline(nodes, cn.map(wchc2chc(_))))
		job
	}
	
	
	def supportedChannelHandlers:Seq[ChannelHandlerDescription] = {
		wsnService.getSupportedChannelHandlers.map(chd2wchd(_))
	}
	
	//-------------- other stuff -------------------------------------------------------------
	
	def addMessageInput(mi:messages.MessageInput) {
		controller.addMessageInput(mi)
	}
	
	def remMessageInput(mi:messages.MessageInput) {
		controller.remMessageInput(mi)
	}
}

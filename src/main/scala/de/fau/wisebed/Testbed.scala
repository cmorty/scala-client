package de.fau.wisebed

import jobs._
import org.slf4j.LoggerFactory
import eu.wisebed.wiseml.WiseMLHelper
import eu.wisebed.api._
import eu.wisebed.api.snaa.AuthenticationTriple
import eu.wisebed.api.controller.Controller
import java.util.GregorianCalendar
import java.util.Calendar
import java.net.InetAddress
import javax.xml.datatype.DatatypeFactory
import scala.collection.JavaConversions._
import WisebedApiConversions._
import eu.wisebed.api.common.KeyValuePair
import de.fau.wisebed.wrappers.WiseML


class Serverconf(val snaaEndpointURL:String, val rsEndpointURL:String, val conf:List[KeyValuePair])

class Testbed(val smEndpointURL:String) {
	protected val log = LoggerFactory.getLogger(this.getClass)

	protected var credentials = List[snaa.AuthenticationTriple]()
	protected var secretAuthenticationKeys = Set[snaa.SecretAuthenticationKey]()
	
	
	protected[wisebed] lazy val sessionManagement = WisebedServiceHelper.getSessionManagementService(smEndpointURL)
	/**
	 * The configuration of the server
	 */

	protected lazy val authenticationSystem = WisebedServiceHelper.getSNAAService(snaaEndpointURL)
	protected lazy val reservationSystem = WisebedServiceHelper.getRSService(rsEndpointURL)
	
	protected lazy val controller = {
		val ec = new WisebedController
		log.debug("Local Testbed-controller published on url: {}", ec.url)
		ec
	}
	
	protected def getMyReservations(reservation: rs.GetReservations):List[Reservation] = {
		reservationSystem.getConfidentialReservations(secretAuthenticationKeys.toSeq, reservation).toList.map(CRD2reservation(_))
	}
	
	
	
	//Public Variables
	lazy val wiseML = new WiseML(sessionManagement.getNetwork)
	
	lazy val serverconf:Serverconf = {
		val rs = new javax.xml.ws.Holder[String]
		val snaa = new javax.xml.ws.Holder[String]
		val options = new javax.xml.ws.Holder[java.util.List[KeyValuePair]]
		sessionManagement.getConfiguration(rs, snaa, options)		
		new Serverconf(snaa.value, rs.value, options.value.toList)
	}
	lazy val snaaEndpointURL:String = serverconf.snaaEndpointURL
	lazy val rsEndpointURL:String = serverconf.rsEndpointURL
	
	
	// Public functions
	/**
	 * Forward wrapper to wiseML.getNodeUrns
	 */
	def getNodes(nodeType:String*):List[String] =  wiseML.getNodeUrns(nodeType)
	def getNodes():List[String] =  wiseML.getNodeUrns()
	
	def getNode(nodeType:String*) =  wiseML.getNodes(nodeType)
	def getNode() =  wiseML.getNodes()
	
	
	def addCredencials(auth:AuthenticationTriple) {
		credentials ::= auth
		updateAuthkeys()
	}
	
	def addCredencials(prefix:String, user:String, password:String) {
		val credentials = new AuthenticationTriple()
		credentials.setUrnPrefix(prefix)
		credentials.setUsername(user)
		credentials.setPassword(password)
		addCredencials(credentials)
	}
	
	def updateAuthkeys() = {
		val authklist = authenticationSystem.authenticate(credentials)
		secretAuthenticationKeys ++= authklist
	}
	

	def getMyReservations(from:GregorianCalendar, to:GregorianCalendar):List[Reservation] = {
		val res = new rs.GetReservations
		res.setFrom(from)
		res.setTo(to)
		getMyReservations(res)
	}
	
	def getMyReservations(min:Int = 30):List[Reservation] = {
		val from = new GregorianCalendar
		val to = new GregorianCalendar
		from.add(Calendar.SECOND, -2)
		to.add(Calendar.MINUTE, min)
		getMyReservations(from, to);
	}
	
	def getReservations(from:GregorianCalendar, to: GregorianCalendar):List[Reservation] = {
		val all = reservationSystem.getReservations(from, to).toList.map(PRD2reservation(_))
		val my = getMyReservations(from, to)
		
		for(r <- my) {
			val myr = all.find(x => {x.user == r.user && x.from == r.from && x.to == r.to})
			myr match  {
				case Some(x)=> x.addKeys(r.secretReservationKeys)
				case None => log.error("Unable to find my Reservation from "  + r.from.toString + "to" + r.to.toString)
			}
		}
		all
	}
	
	def getReservations(min:Int = 30):List[Reservation] = {
		val from = new GregorianCalendar
		val to = new GregorianCalendar
		from.add(Calendar.SECOND, -2)
		to.add(Calendar.MINUTE, min)
		getReservations(from, to)
	}
	
	def makeReservation(res:Reservation):Reservation = {
		res addKeys reservationSystem.makeReservation(secretAuthenticationKeys.toSeq, res.asConfidentialReservationData)
		res
	}
	
	def makeReservation(from:GregorianCalendar, to: GregorianCalendar, nodeUrns:Seq[String], user:String = credentials.head.getUsername):Reservation = {
		val res = new Reservation(from, to, nodeUrns, user)
		makeReservation(res)
	}
	
	def deleteReservation(res:Reservation) {
		reservationSystem.deleteReservation(secretAuthenticationKeys.toSeq, res.secretReservationKeys)
	}
	
	def freeReservation(res:Reservation) {
		sessionManagement.free(res.sm_reservationkeys)
	}
	
	def areNodesAlive(nodes:Seq[String]):NodesAliveJob = {
		val job = new NodesAliveJob(nodes)		
		val url = controller.url
		sessionManagement.areNodesAlive(nodes, url)
		job		
	}
	
	def getNetwork():String = {
		sessionManagement.getNetwork()
	}
	
	
}

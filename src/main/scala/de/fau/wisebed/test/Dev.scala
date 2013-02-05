package de.fau.wisebed.test


import de.uniluebeck.itm.tr.util.Logging
import java.io.File
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import scala.xml.XML
import de.fau.wisebed.Testbed
import java.util.GregorianCalendar
import java.util.Calendar
import de.fau.wisebed.Reservation.reservation2CRD
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions._
import de.fau.wisebed.Experiment
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.messages.MsgLiner
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.wrappers
import de.fau.wisebed.jobs.MoteAliveState._
import de.fau.wisebed.wrappers.WrappedChannelHandlerConfiguration
import de.fau.wisebed.Reservation


object Dev {
	def main(args: Array[String]) {
		Logging.setLoggingDefaults(Level.DEBUG) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

		var activemotes: List[String] = null
		var exp: Experiment = null
		var res: List[Reservation] = null

		val log = LoggerFactory.getLogger(this.getClass)
		val conffile = new File("config.xml")
		if (!conffile.exists) {
			log.error("Could not find \"config.xml\"");
			sys.exit(1)
		}

		val conf = XML.loadFile(conffile)
		val smEndpointURL = (conf \ "smEndpointURL").text.trim
		

		val prefix = (conf \ "prefix").text.trim
		val login = (conf \ "login").text.trim
		val pass = (conf \ "pass").text.trim

		//Get Motes
		log.debug("Starting Testbed")
		val tb = new Testbed(smEndpointURL)
		
		log.debug("snaaEndpointURL: " + tb.snaaEndpointURL)
		log.debug("rsEndpointURL: " + tb.rsEndpointURL)
		log.debug("Conf: " + tb.serverconf.conf.map(x => {x.getKey + ": " +x.getValue}).mkString(", "))
		
		
		//log.debug("Network: " + tb.getNetwork)
		
		log.debug("Requesting Motes")
		val motes = tb.getNodes()
		log.debug("Motes: " + motes.mkString(", "))
		try {
			val alivem = tb.areNodesAlive(motes)()
			log.debug("Motestates: " + alivem.map(x => {x._1 + ": " +x._2}).mkString(", "))
		} catch {
			case e: Exception => println("exception caught: " + e.printStackTrace())
			Thread.sleep(1000*600)
		} 
		
	}
}
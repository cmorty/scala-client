package de.fau.wisebed.test

import java.io.File

import scala.xml.XML

import org.slf4j.LoggerFactory

import de.fau.wisebed.Experiment
import de.fau.wisebed.Reservation
import de.fau.wisebed.Testbed
import de.fau.wisebed.util.Logging.setDefaultLogger


object Dev {
	def main(args: Array[String]) {
		//Logging.setLoggingDefaults(Level.DEBUG) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))
		
		setDefaultLogger
		//Logger.getRootLogger().addAppender(new ConsoleAppender(layout));

		var activenodes: List[String] = null
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

		//Get Nodes
		log.debug("Starting Testbed")
		val tb = new Testbed(smEndpointURL)
		
		log.debug("snaaEndpointURL: " + tb.snaaEndpointURL)
		log.debug("rsEndpointURL: " + tb.rsEndpointURL)
		log.debug("Conf: " + tb.serverconf.conf.map(x => {x.getKey + ": " +x.getValue}).mkString(", "))
		
		
		//log.debug("Network: " + tb.getNetwork)
		
		log.debug("Requesting Nodes")
		val nodes = tb.getNodes()
		log.debug("Nodes: " + nodes.mkString(", "))
		try {
			val alivem = tb.areNodesAlive(nodes)()
			log.debug("Nodestate: " + alivem.map(x => {x._1 + ": " +x._2}).mkString(", "))
		} catch {
			case e: Exception => println("exception caught: " + e.printStackTrace())
			Thread.sleep(1000*600)
		} 
		
	}
}
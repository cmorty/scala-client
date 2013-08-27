package de.fau.wisebed.test

import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import scala.xml.XML
import org.slf4j.LoggerFactory
import de.fau.wisebed.Experiment
import de.fau.wisebed.Reservation
import de.fau.wisebed.Testbed
import de.fau.wisebed.jobs.NodeAliveState.Alive
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.messages.MsgLiner
import de.fau.wisebed.WisebedApiConversions._
import de.fau.wisebed.wrappers.ChannelHandlerConfiguration


object TH {
	//Add handler to shut down everything in case of an accident
	val handler = new ExHandler();
	Thread.setDefaultUncaughtExceptionHandler(handler);


	var activeNodes:List[String] = null
	var exp:Experiment = null
	var res:List[Reservation] = null
	
	
	val log = LoggerFactory.getLogger(this.getClass)
	val conffile = new File("config.xml")
	if(!conffile.exists){
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
	
	
	log.debug("Requesting Nodes")
	val nodes = tb.getNodes()
	log.debug("Nodes: " + nodes.mkString(", "))
	
	log.debug("Logging in")
	tb.addCredencials(prefix, login, pass)
	
	/**
	 * @param resTime Reserve nodes for a certain amount of time.
	 */
	def init(resTime:Int){
		log.debug("Requesting reservations")
		res = tb.getReservations(resTime)
		
		for(r <- res) {
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.nodeURNs.mkString(", ")) 
		}
		
		if(!res.exists(_.now)) {
			log.debug("No Reservations or in the Past- Requesting")
			val from = new GregorianCalendar
			from.add(Calendar.MINUTE, -1)
			val to = new GregorianCalendar
			to.add(Calendar.MINUTE, resTime)
			val r = tb.makeReservation(from, to, nodes, "morty")
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.nodeURNs.mkString(", ")) 
			res ::= r
		} else if(!res.find(_.now).get.mine) {
			log.error("Someone else has an reservation!")
		}
		
		exp = new Experiment(res.toList, tb)
		
		
		exp.addMessageInput(  new MessageLogger(mi => {
			log.info("Got message from " + mi.node + ": " + mi.dataString)
		}) with MsgLiner)
		
		log.debug("Requesting Nodestate")
		val statusj = exp.areNodesAlive(nodes)
		val status = statusj.status
		for((m, s) <- status) log.info(m +": " + s)
		
		activeNodes = (for((m, s) <- status; if(s == Alive)) yield m).toList
		
		log.debug("Requesting Supported Channel Handlers")
		val handls = exp.supportedChannelHandlers
		if(false)
			for(h <- handls){
				println(h.format)
			}
		
		val setHand = "contiki"
		
		if(handls.find(_.name == setHand) == None){
			log.error("Can not set handler: {}", setHand)
			sys.exit(1)
		} else {
			log.debug("Setting Handler: {}", setHand)
			val chd = exp.setChannelHandler(activeNodes, new ChannelHandlerConfiguration("contiki") )
			if(!chd.success){
				log.error("Failed setting Handler")
				sys.exit(1)
			}
		}
	}
	
	def flash(file:String){
		log.debug("Flashing " + file)
		val flashj = exp.flash(file, activeNodes)
		if(!flashj.success){
			log.error("Failed to flash nodes")
			sys.exit(1)
		}
	}
	
	def reset{
		log.debug("Resetting")
		val resj = exp.resetNodes(activeNodes)
		if(!resj.success){
			log.error("Failed to reset nodes")
			sys.exit(1)
		}
	}
	
	def finish{
		log.debug("Removing Reservation")
		res.foreach(tb.freeReservation(_))
		
		Thread.sleep(1000 * 4)
		
		val st = Thread.getAllStackTraces
           
        for(t <- st){
            if(t._1.isDaemon()){
                println("Deamon: " + t._1.toString)
            } else {
                println("Thread: " +  t._1.toString)
                t._2.foreach(println(_))
            }
        }
		
	}
}


class ExHandler extends Thread.UncaughtExceptionHandler {
  def uncaughtException( t:Thread,  e:Throwable) {
  	System.err.println("Throwable: " + e.getMessage());
    System.err.println(t.toString());
    System.err.println("Terminating");
    sys.exit(1)
  }
}


package de.fau.wisebed.test

import org.slf4j.LoggerFactory

import de.fau.wisebed.messages.MessageWaiter
import de.fau.wisebed.util.Logging.setDefaultLogger



object Flash {
	

	
		
	def main(args: Array[String]) {
		setDefaultLogger
		
		val log = LoggerFactory.getLogger(this.getClass)
		
		
		TH.init(30)
		
		
		TH.flash("sky-shell.ihex")
		
		
		val bw = new MessageWaiter(TH.activeNodes,  "Contiki>")
		TH.exp.addMessageInput(bw)
		

		
		log.debug("Waiting for bootup")
		if(! bw.waitResult(10*1000)){
			log.error("Boot failed");
			sys.exit(1)
		}
		
		
		TH.finish
		
		
		log.debug("DONE")
		sys.exit(0)
	}
}

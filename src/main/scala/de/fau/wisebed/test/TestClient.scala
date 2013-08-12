package de.fau.wisebed.test

import org.slf4j.LoggerFactory

import de.fau.wisebed.messages.MessageWaiter

object TestClient {
	val log = LoggerFactory.getLogger(this.getClass)

	
		
	def main(args: Array[String]) {

		
		TH.init(30)
		TH.flash("sky-shell.ihex")
		
		
		


		val bw = new MessageWaiter(TH.activemotes,  "Contiki>")
		TH.exp.addMessageInput(bw)
		
		
		TH.reset
		
		log.debug("Waiting for bootup")
		if(! bw.waitTimeout(10*1000)){
			log.error("Boot failed");
			sys.exit(1)
		}
		
		log.debug("Sending \\n")
		val snd = TH.exp.send(TH.activemotes, "help\n")
		if(!snd.success){
			log.error("Failed to send information to nodes")
			sys.exit(1)
		}
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		

		
		
		log.debug("DONE")
		sys.exit(0)
	}
}

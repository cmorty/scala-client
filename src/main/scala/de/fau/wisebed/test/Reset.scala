package de.fau.wisebed.test

import org.slf4j.LoggerFactory

object Reset {
	val log = LoggerFactory.getLogger(this.getClass)

	
		
	def main(args: Array[String]) {
		
		
		TH.init(30)
		
		
		TH.reset
		
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		
		TH.finish
		
		
		log.debug("DONE")
		sys.exit(0)
	}
}

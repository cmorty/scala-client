package de.fau.wisebed.test

import org.slf4j.LoggerFactory


object MakeReservation {

	var usage = "java -cp thisfile de.fau.wisebed.test.MakeReservation <time in min> "
	
	def main(args: Array[String]) {
		val log = LoggerFactory.getLogger(this.getClass)
		
		
		if(args.length != 1){
			log.info(usage)
			return
		}
		val time = try{
			args(0).toInt
		} catch {
			case e:Throwable => {
				log.info(usage)
				return
			}
		}
		
		TH.init(time)				
		TH.finish
		log.debug("DONE")
		sys.exit(0)
	}

}
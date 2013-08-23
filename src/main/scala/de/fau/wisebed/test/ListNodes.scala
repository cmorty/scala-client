package de.fau.wisebed.test

import org.slf4j.LoggerFactory

object ListNodes {
	val log = LoggerFactory.getLogger(getClass)
		
	def main(args: Array[String]) {
		TH.init(2)

		val inact = TH.nodes.filter(!TH.activeNodes.contains(_))
		log.info("Aktive Nodes  : " +  TH.activeNodes.mkString(", "))
		log.info("Inaktive Nodes: " +  inact.mkString(", "))

		TH.finish
	}
}

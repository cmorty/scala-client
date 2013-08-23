package de.fau.wisebed.wrappers

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.implicitConversions

import org.slf4j.LoggerFactory

import de.fau.wisebed.WisebedApiConversions.kvp2map
import eu.wisebed.api.wsn

class ChannelHandlerDescription(chd:wsn.ChannelHandlerDescription) {
	val log = LoggerFactory.getLogger(ChannelHandlerDescription.this.getClass)
	def name:String = chd.getName
	def description:String = chd.getDescription
	def configuration:Map[String,String] = chd.getConfigurationOptions.toList
	
	def format:String = {
		val rv = new StringBuilder() 
		rv ++= name + ": " + description + "\n"
		for(c <- configuration) {
			rv ++= "\t" + c._1 + "\t" + c._2  + "\n"
		}
		rv.toString
	}
	
	def channelHandlerDescription = chd
}


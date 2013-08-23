package de.fau.wisebed.wrappers


import eu.wisebed.api.wsn

class ChannelHandlerConfiguration(chc:wsn.ChannelHandlerConfiguration = new wsn.ChannelHandlerConfiguration) {
	val wconf = new KeyValuePairMap(chc.getConfiguration)
	
	def this (name:String)  {
		this()
		chc.setName(name)
	}
	
	def channelHandlerConfiguration = chc
	
	def name:String = chc.getName
	def name_=(n:String) {chc.setName(n)}
	
	def conf(k:String):Option[String] = wconf.get(k)
	def conf_=(k:String, v:String)  = wconf += k -> v
	
	def getConfMap = wconf	
}


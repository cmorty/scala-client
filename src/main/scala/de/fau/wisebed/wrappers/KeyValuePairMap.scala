package de.fau.wisebed.wrappers

import scala.collection._
import eu.wisebed.api.common
import scala.collection.JavaConversions._

class KeyValuePairMap(_ls:java.util.List[common.KeyValuePair]) extends mutable.Map[String, String] {
    def ls = asScalaBuffer(_ls)
	
	def get(key: String): Option[String] = ls.find(_.getKey == key) map (_.getValue)
	def iterator: Iterator[(String, String)] = ls.map(x => {x.getKey -> x.getValue}).iterator
	
	def += (kv: (String, String)) = { 
		remove(kv._1)
		val kvp = new common.KeyValuePair 
		kvp.setKey( kv._1)
		kvp.setValue(kv._2)
		ls.append(kvp)
		KeyValuePairMap.this 
	}
	
	def -= (key: String) = { remove(key); KeyValuePairMap.this }

	private def remkey(key:String)  {
		ls.filter(_.getKey == key).foreach(ls -= _)
	}
}

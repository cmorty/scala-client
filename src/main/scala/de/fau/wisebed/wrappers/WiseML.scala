package de.fau.wisebed.wrappers

import javax.xml.bind.JAXB
import java.io.StringReader
import eu.wisebed.wiseml.Setup
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import de.fau.wisebed.WisebedApiConversions._

class WiseML(serializedWiseML:String) {
	lazy private val wiseml = JAXB.unmarshal(new StringReader(serializedWiseML), classOf[eu.wisebed.wiseml.Wiseml])
	
	 /**
	 * Reads out all nodes that are contained in the setup-part of the document.
	 *
	 * @param types
	 * 		node types to include, e.g. "isense", "telosb" will include all iSense and all TelosB nodes contained
	 * 		in the WiseML document
	 *
	 * @return a List of {@link Setup.Node} instances
	 */
	
	def getNodes(types:Seq[String] = null):List[Node] = {	
		if (types == null || types.isEmpty) wiseml.getSetup.getNode.map(new Node(_)).toList
		else wiseml.getSetup.getNode.filter(x => {val id = x.getNodeType; types.find(_.equalsIgnoreCase(id)) != None}).map(new Node(_)).toList		
	}
	/**
	 * Allows passing a list rather than a Seq.
	 */
	def getNodes(ntype:String, ntypes:String*):List[Node] = getNodes(ntype +: ntypes)
	
	def getNodeUrns(types:Seq[String] = null):List[String] = getNodes(types).map(_.id)
	def getNodeUrns(ntype:String, ntypes:String*):List[String] = getNodeUrns(ntype +: ntypes)
	

	
	
}
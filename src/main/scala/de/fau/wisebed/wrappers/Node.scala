package de.fau.wisebed.wrappers

import eu.wisebed.wiseml.Setup
import scala.language.implicitConversions

class Node(_node:Setup.Node) {

	def node = _node
	def id:String = node.getId	
	def programDetails = node.getProgramDetails
	def nodeType = node.getNodeType
	def description = node.getDescription
	override def toString = id
}


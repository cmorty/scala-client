package de.fau.wisebed.wrappers

import eu.wisebed.wiseml.Setup
import scala.language.implicitConversions

class Node(_node:Setup.Node) {

	def node = _node
	def id = node.getId	
	def programDetails = node.getProgramDetails
	def nodeType = node.getNodeType
	def description = node.getDescription
}


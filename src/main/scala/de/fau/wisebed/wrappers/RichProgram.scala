package de.fau.wisebed.wrappers

import eu.wisebed.api.wsn.Program
import eu.wisebed.api.wsn.ProgramMetaData
import java.io.File
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream


/**
 * This needs refactoring!
 */
class RichProgram extends Program {}

object RichProgram {
	
	private def loadprog( is:InputStream,  name:String, other:String,   platform:String,  version:String):Program = {
		val rv = new Program
		val programMetaData = new ProgramMetaData
		programMetaData.setName(name)
		programMetaData.setOther(other)
		programMetaData.setPlatform(platform)
		programMetaData.setVersion(version)
		
		val dis = new BufferedInputStream(is)

		val binaryData = Stream.continually(dis.read).takeWhile(-1 != _).map(_.toByte).toArray

		rv.setProgram(binaryData)
		rv.setMetaData(programMetaData)
		rv
	}
	
	def loadStream(is:InputStream,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):Program =
		loadprog(is,  name, other,  platform,  version)
	
	
	
  	def apply( pathName:String,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):Program = {
		val fis = new FileInputStream(new File(pathName))		
		loadprog(fis,  name, other,  platform,  version)
	}
}

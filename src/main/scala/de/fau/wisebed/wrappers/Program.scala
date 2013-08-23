package de.fau.wisebed.wrappers

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

import eu.wisebed.api.wsn
import eu.wisebed.api.wsn.ProgramMetaData


class Program(_program:wsn.Program) {
	def program = _program
	def metaData = program.getMetaData
	def name = metaData.getName
	def other = metaData.getOther
	def platform = metaData.getPlatform
	def version = metaData.getVersion
}

object Program {
			
	def apply(is:InputStream,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):Program = {
		val prog = new wsn.Program
		val programMetaData = new ProgramMetaData
		programMetaData.setName(name)
		programMetaData.setOther(other)
		programMetaData.setPlatform(platform)
		programMetaData.setVersion(version)
		
		val dis = new BufferedInputStream(is)

		val binaryData = Stream.continually(dis.read).takeWhile(-1 != _).map(_.toByte).toArray

		prog.setProgram(binaryData)
		prog.setMetaData(programMetaData)

		new Program(prog)
	
	}
	
  	def loadFile( pathName:String,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):Program = {
		val fis = new FileInputStream(new File(pathName))		
		apply(fis,  name, other,  platform,  version)
	}
	
  	def loadStream( is:InputStream,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):Program = 
  		apply(is,  name, other,  platform,  version)
  	
  	

}

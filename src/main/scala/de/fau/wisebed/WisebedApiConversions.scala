package de.fau.wisebed

import java.util.GregorianCalendar
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import eu.wisebed.api.common
import eu.wisebed.api.rs
import eu.wisebed.api.snaa
import eu.wisebed.api.sm
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import de.fau.wisebed.wrappers._
import eu.wisebed.api.wsn
import java.io.FileInputStream
import java.io.File
import eu.wisebed.wiseml.Setup
import eu.wisebed.api.rs.ConfidentialReservationData
import scala.collection.JavaConversions._
import scala.collection.mutable.Buffer
import eu.wisebed.api.rs.PublicReservationData

object WisebedApiConversions {
	
	//KeyValuePair
	implicit def kvp2map(kvp:List[common.KeyValuePair]):Map[String, String] = kvp.map(x => (x.getKey -> x.getValue)).toMap
	
	implicit def map2kvp(m:Map[String,String]):List[common.KeyValuePair] = m.map {
		case (k, v) => {
			val rv = new common.KeyValuePair
			rv.setKey(k)
			rv.setValue(v)
			rv
		}
	}.toList
	
	
	//Authentification
	implicit def secretAuthentificationKey_snaa2rs(snaaKey:snaa.SecretAuthenticationKey):rs.SecretAuthenticationKey = {
		val key = new rs.SecretAuthenticationKey
		key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey)
		key.setUrnPrefix(snaaKey.getUrnPrefix)
		key.setUsername(snaaKey.getUsername)
		key
	}
	
	implicit def secretAuthentificationKeys_snaa2rs(snaaKeys:Iterable[snaa.SecretAuthenticationKey]):java.util.List[rs.SecretAuthenticationKey] = {
		snaaKeys.map(secretAuthentificationKey_snaa2rs).toSeq
	}

	implicit def data2Key(dat: Iterable[rs.Data]): Iterable[rs.SecretReservationKey] = dat.map(x => {
		val rv = new rs.SecretReservationKey
		rv.setSecretReservationKey(x.getSecretReservationKey)
		rv.setUrnPrefix(x.getUrnPrefix)
		rv
	})
	
	implicit def greg2XMLGreg(greg: GregorianCalendar):XMLGregorianCalendar = {
		DatatypeFactory.newInstance().newXMLGregorianCalendar(greg)
	}

	
	//ChannelHandlerConfiguration	
	implicit def chc2wchc(chc:wsn.ChannelHandlerConfiguration) = new ChannelHandlerConfiguration(chc)
	implicit def wchc2chc(wchc:ChannelHandlerConfiguration) = wchc.channelHandlerConfiguration
	
	// ChannelHandlerDescription
	implicit def chd2wchd(chd:wsn.ChannelHandlerDescription):ChannelHandlerDescription = new ChannelHandlerDescription(chd)
	implicit def wchd2chd(rchd:ChannelHandlerDescription):wsn.ChannelHandlerDescription = rchd.channelHandlerDescription
	
	
	//Program	
	implicit def program2wprogram(p:wsn.Program):Program = new Program(p)
	implicit def wprogram2program(wp:Program):wsn.Program = wp.program
	implicit def file2is(pathName:String) =  new FileInputStream(new File(pathName))
	
	//Reservation
		
	implicit def reservation2CRD(res:Reservation):rs.ConfidentialReservationData = res.asConfidentialReservationData
	
	implicit def CRD2reservation(res:ConfidentialReservationData):Reservation = {
		new Reservation(res.getFrom.toGregorianCalendar, res.getTo.toGregorianCalendar, res.getNodeURNs.toList, res.getUserData,data2Key(res.getData))
	}
	
	implicit def PRD2reservation(res:PublicReservationData):Reservation = {
		new Reservation(res.getFrom.toGregorianCalendar, res.getTo.toGregorianCalendar, res.getNodeURNs, res.getUserData)
		
	}
	
	
	implicit def secretReservationKey_Rs2SM(rsKs: Traversable[Reservation]): java.util.List[sm.SecretReservationKey] = {
		rsKs.map(_.sm_reservationkeys).flatten.toList
	}
	
	
	
	
	//Message
	implicit def message2wmessage(msg:common.Message):Message = new Message(msg)
	implicit def wmessage2message(wmsg:Message):common.Message = wmsg.msg
	
	//Node
	implicit def node2wnode(node:Setup.Node):Node = new Node(node)
	implicit def wnode2node(wnode:Node):Setup.Node = wnode.node
	implicit def wnode2str(wnode:Node):String = wnode.id
	
	
	
}

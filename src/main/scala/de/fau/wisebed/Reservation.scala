package de.fau.wisebed

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.Buffer

import org.slf4j.LoggerFactory

import WisebedApiConversions._
import eu.wisebed.api.rs
import eu.wisebed.api.sm


class Reservation(_from:GregorianCalendar, _to:GregorianCalendar,_nodeURNs:Seq[String], user:String, keys:Iterable[rs.SecretReservationKey] = null) {
	val log = LoggerFactory.getLogger(this.getClass)

	val lfrom:GregorianCalendar = _from.clone.asInstanceOf[GregorianCalendar]
	val lto = _to.clone.asInstanceOf[GregorianCalendar]
    
	def from:GregorianCalendar = lfrom.clone.asInstanceOf[GregorianCalendar]
	def to:GregorianCalendar = lto.clone.asInstanceOf[GregorianCalendar]
    
	def nodeURNs = _nodeURNs.toList
	
	@deprecated("Use nodeURNs", "Now")
	def getNodeURNs = nodeURNs
	
	
	var userData:String = ""
	val secretReservationKeys = Buffer[rs.SecretReservationKey]()
	if(keys != null) {
		secretReservationKeys ++= keys
	}
    
	def inThePast = to.before(new GregorianCalendar)    
	def now = {
		val t = new GregorianCalendar
		lfrom.before(t) && lto.after(t)
	}

	def addKeys(keys:Iterable[rs.SecretReservationKey]):Unit = {
		secretReservationKeys ++= keys
	}
	
	def asConfidentialReservationData:rs.ConfidentialReservationData = { 
		val rv = new rs.ConfidentialReservationData
		rv.setFrom(lfrom)
		rv.setTo(lto)
		rv.setUserData(user)
		rv.getNodeURNs.addAll(nodeURNs)
		rv
	}

	def copy():Reservation = {
 		val rv = new Reservation(lfrom, lto, nodeURNs, user)
 		rv.addKeys(secretReservationKeys)
 		rv
 	}
 	
 	def dateString(format:String = "HH:mm:ss", split:String = " - "):String = {
		val f = new SimpleDateFormat(format)
		f.format(from.getTime) + split + f.format(to.getTime)
 	}
 	
 	
 	private def secretReservationKey_Rs2SM(rsKs: Traversable[rs.SecretReservationKey]): Buffer[sm.SecretReservationKey] = {
		val rv = Buffer[sm.SecretReservationKey]()
		for(rsK <- rsKs) {
			val rk = new sm.SecretReservationKey
			rk.setSecretReservationKey(rsK.getSecretReservationKey)
			rk.setUrnPrefix(rsK.getUrnPrefix)
			rv += rk
		}
		rv
	}
 	
 	def sm_reservationkeys:List[sm.SecretReservationKey] = {
 		secretReservationKey_Rs2SM(secretReservationKeys).toList
 	}
}


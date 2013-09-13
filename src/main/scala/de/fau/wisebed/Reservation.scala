package de.fau.wisebed

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.Buffer

import org.slf4j.LoggerFactory

import WisebedApiConversions._
import eu.wisebed.api.rs
import eu.wisebed.api.sm


class Reservation(_from:GregorianCalendar, _to:GregorianCalendar,_nodeURNs:Seq[String], _user:String, keys:Iterable[rs.SecretReservationKey] = null) {
	val log = LoggerFactory.getLogger(this.getClass)

	val lfrom:GregorianCalendar = _from.copy
	val lto = _to.copy
    
	def from:GregorianCalendar = lfrom.copy
	def to:GregorianCalendar = lto.copy
	def user = _user
    
	def nodeURNs = _nodeURNs.toList
	
	@deprecated("Use nodeURNs", "Now")
	def getNodeURNs = nodeURNs
	
	
	var userData:String = ""
	val _secretReservationKeys = Buffer[rs.SecretReservationKey]()

	
	if(keys != null) {
		_secretReservationKeys ++= keys
	}
    
	def secretReservationKeys = _secretReservationKeys
	def inThePast = to.before(new GregorianCalendar)    
	def now = {
		val t = new GregorianCalendar
		lfrom.before(t) && lto.after(t)
	}
	
	def mine =  _secretReservationKeys.size > 0
	
	def addKeys(keys:Iterable[rs.SecretReservationKey]):Unit = {
		_secretReservationKeys ++= keys
	}
	
	def asConfidentialReservationData:rs.ConfidentialReservationData = { 
		val rv = new rs.ConfidentialReservationData
		rv.setFrom(lfrom)
		rv.setTo(lto)
		rv.setUserData(_user)
		rv.getNodeURNs.addAll(nodeURNs)
		rv
	}

	def copy():Reservation = {
 		val rv = new Reservation(lfrom, lto, nodeURNs, _user)
 		rv.addKeys(_secretReservationKeys)
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
 		secretReservationKey_Rs2SM(_secretReservationKeys).toList
 	}
}


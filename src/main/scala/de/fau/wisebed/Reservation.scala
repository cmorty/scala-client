package de.fau.wisebed

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.Buffer

import org.slf4j.LoggerFactory

import WisebedApiConversions._
import eu.wisebed.api.rs
import eu.wisebed.api.sm


class Reservation(_from:GregorianCalendar, _to:GregorianCalendar,val nodeURNs:Seq[String], val user:String, keys:Iterable[rs.SecretReservationKey] = null) {
	protected val log = LoggerFactory.getLogger(this.getClass)

	
	//Gregorian calander is not constant
	protected val lfrom:GregorianCalendar = _from.copy
	protected val lto = _to.copy
	
	def from:GregorianCalendar = lfrom.copy
	def to:GregorianCalendar = lto.copy
	
	@deprecated("Use nodeURNs", "Now")
	def getNodeURNs = nodeURNs
	
	
	var userData:String = ""
	private val _secretReservationKeys = Buffer[rs.SecretReservationKey]()
	
	
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
		rv.setUserData(user)
		rv.getNodeURNs.addAll(nodeURNs)
		rv
	}

	def copy():Reservation = {
 		new Reservation(lfrom, lto, nodeURNs, user, _secretReservationKeys)
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


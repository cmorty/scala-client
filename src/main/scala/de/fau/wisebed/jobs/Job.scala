package de.fau.wisebed.jobs

import scala.concurrent.Future
import eu.wisebed.api.controller.Status
import eu.wisebed.api.controller.RequestStatus
import scala.actors.Actor
import org.slf4j.Logger
import scala.actors.TIMEOUT
import scala.actors.OutputChannel
import de.fau.wisebed.RemJob
import de.fau.wisebed.StopAct
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.CanAwait
import java.util.concurrent.TimeoutException

abstract class Job[S](nodes: Seq[String]) extends Actor with Future[Map[String, S]] {
	val log:Logger
	
	private val  prom =  Promise[Map[String, S]]()
	
	private[jobs] val states = Map[String, Promise[S]](nodes.map(_ -> Promise[S]()) : _*)
	private[jobs] def update(node: String, v:Int, msg:String):Option[S]
	
	val successValue: S

	private def isDone:Boolean = states.values.forall(_.isCompleted)
	def isCompleted = prom.future.isCompleted
	
	def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
	 	if(prom.future.ready(atMost).isCompleted) this
	 	else throw new TimeoutException("Futures timed out after [" + atMost + "]")
	}

	def statusUpdate(s:Status) {
		log.debug("Got state for " + s.getNodeId + ": " + s.getValue)
		update(s.getNodeId, s.getValue, s.getMsg) match {
			case Some(stat) => {
				if(!states.contains(s.getNodeId)) log.error("Got answer for unrequested node: " + s.getNodeId) 
				else states(s.getNodeId).complete( new Success(stat))
			}
			case None => // no status update
		}
	}

	def act(){
		log.debug("Job actor started: {}", this.toString)
		loop {
			react{				
				case s:Status =>
					statusUpdate(s)			
					if(isDone) {
						prom.complete(new Success(apply))  						
						sender ! RemJob(this)
					}
				case StopAct =>
					log.debug("Job actor stopped: {}", this.toString)
					exit
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}
	}
	
	def result(atMost: Duration)(implicit permit: CanAwait) = prom.future.result(atMost)
	def value = prom.future.value
	def onComplete[U](func: (Try[Map[String, S]]) ⇒ U)(implicit executor: ExecutionContext) = prom.future.onComplete(func)
	
	def apply():Map[String, S] = {
		states.mapValues(x => {Await.result(x.future, Duration.Inf)})
	}
		
	def status = apply

	def success:Boolean = apply().values.forall(_ == successValue)
}


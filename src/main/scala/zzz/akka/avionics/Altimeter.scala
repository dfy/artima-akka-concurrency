package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import scala.concurrent.util.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Altimeter {
	case class RateChange(amount: Float)
	case class AltitudeUpdate(altitude: Double)

	def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging with ProductionEventSource { //this: EventSource =>
	import Altimeter._

	val ceiling = 4300

	val maxRateOfClimb = 5000

	var rateOfClimb: Float = 0

	var altitude: Double = 0

	var lastTick = System.currentTimeMillis

	val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self, Tick)

	case object Tick

	def altimeterReceive: Receive = {
		case RateChange(amount) =>
			rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
			//log.info("Altimeter RateChange : " + amount)
			log.info("Altimeter changed rate of climb to : " + rateOfClimb)

		case Tick =>
			val tick = System.currentTimeMillis
			altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
			lastTick = tick
			//log.info("Tick : " + rateOfClimb)
			sendEvent(AltitudeUpdate(altitude))
	}

	def receive = eventSourceReceive orElse altimeterReceive

	override def postStop(): Unit = ticker.cancel
}
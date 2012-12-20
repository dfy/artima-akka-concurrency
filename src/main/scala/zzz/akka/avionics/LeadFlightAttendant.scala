package zzz.akka.avionics
import akka.actor.{Actor, ActorRef, Props}

trait AttendantCreationPolicy {
	val numberOfAttendants: Int = 8
	def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
	def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
	case object GetFlightAttendant
	case class Attendant(a: ActorRef)
	def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor { this: AttendantCreationPolicy => 
	import LeadFlightAttendant._

	override def preStart() {
		import scala.collection.JavaConverters._

		val attendantNames = context.system.settings.config.getStringList(
			"zzz.akka.avionics.flightcrew.attendantNames").asScala

	}
}

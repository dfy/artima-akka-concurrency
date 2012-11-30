package zzz.akka.avionics

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
import scala.concurrent.util.duration._
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.scalatest.{WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.MustMatchers

object EventSourceSpy {
	val latch = new CountDownLatch(1)
}

trait EventSourceSpy extends EventSource {
	override def sendEvent[T](event: T): Unit = EventSourceSpy.latch.countDown()

	override def eventSourceReceive = { case "" => }
}

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
	with ImplicitSender
	with WordSpec
	with MustMatchers
	with BeforeAndAfterAll {

	import Altimeter._
	override def afterAll() { system.shutdown() }

	def slicedAltimeter = new Altimeter with EventSourceSpy

	def actor() = {
		val a = TestActorRef[Altimeter](Props(slicedAltimeter))
		(a, a.underlyingActor)
	}

	"Altimeter" should {

		"record rate of climb changes" in {
			val (_, real) = actor()
			real.receive(RateChange(1f))
			real.rateOfClimb must be (real.maxRateOfClimb)
		}

		"keep rate of climb changes within bounds" in {
			val (_, real) = actor()
			real.receive(RateChange(2f))
			real.rateOfClimb must be (real.maxRateOfClimb)
		}

		"calculate altitude changes" in {
			val ref = system.actorOf(Props(Altimeter()))
			ref ! EventSource.RegisterListener(testActor)
			ref ! RateChange(0.9f)
			fishForMessage() {
				case AltitudeUpdate(altitude) if (altitude) == 0f => false
				case AltitudeUpdate(altitude) => true
			}
		}

		"send events" in {
			val (ref, _) = actor()
			EventSourceSpy.latch.await(1, TimeUnit.SECONDS) must be (true)
		}
	}

} 
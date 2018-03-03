/*
 * Copyright (c) 2017 joesan @ http://github.com/joesan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.inland24.plantsim.core

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.inland24.plantsim.config.AppConfig
import com.inland24.plantsim.models.PowerPlantConfig.OnOffTypeConfig
import com.inland24.plantsim.models.PowerPlantType.OnOffType
import com.inland24.plantsim.services.database.DBServiceSpec
import com.inland24.plantsim.services.simulator.onOffType.OnOffTypeActor
import com.inland24.plantsim.services.simulator.onOffType.OnOffTypeActor.Config
import com.inland24.plantsim.streams.EventsStream
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.inland24.plantsim.models.PowerPlantSignal.Transition
import com.inland24.plantsim.models.PowerPlantState.{Active, Init}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

// TODO: Under implementation
//@Ignore
class EventsWebSocketActorTest
    extends TestKit(ActorSystem("EventsWebSocketActorTest"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with DBServiceSpec {

  override def beforeAll: Unit = {
    // 1. Set up the Schemas
    super.h2SchemaSetup()

    // 2. Populate the tables
    super.populateTables()
  }

  override def afterAll {
    System.clearProperty("ENV")
    super.h2SchemaDrop()
    TestKit.shutdownActorSystem(system)
  }

  // Use a test AppConfig
  // (We test against application.test.conf - See DBServiceSpec) where we
  // set this as Environment variable
  val appCfg = AppConfig.load()
  implicit val ec = monix.execution.Scheduler.Implicits.global

  // This will be our PowerPlantActor instance
  val onOffTypeCfg = OnOffTypeConfig(
    102,
    "joesan 102",
    200.0,
    1600.0,
    OnOffType
  )
  val powerPlantObservable = PowerPlantEventObservable(ec)

  // This will be the channel which our PowerPlantActor will use to push messages
  val publishChannel = system.actorOf(EventsStream.props(powerPlantObservable))
  val powerPlantActor = system.actorOf(
    OnOffTypeActor.props(Config(onOffTypeCfg, Some(publishChannel)))
  )

  // This is our buffer to which we can save and check the test expectations
  val buffer = ListBuffer.empty[String]

  // This will be our sink to which the publishChannel will pipe messages to the WebSocket endpoint
  class SinkActor extends Actor {
    override def receive: Receive = {
      case jsonStr: String =>
        buffer += jsonStr
    }
  }
  val sink = system.actorOf(Props(new SinkActor))

  "EventsWebSocketActor # telemetrySignals" must {
    // Let us create our EventsWebSocketActor instance (for TelemetrySignals)
    val telemetrySignalsWebSocketActor = system.actorOf(
      EventsWebSocketActor.props(
        EventsWebSocketActor.telemetrySignals(102, powerPlantActor),
        sink
      )
    )

    // Let us create our EventsWebSocketActor instance (for Events)
    val eventsWebSocketActor = system.actorOf(
      EventsWebSocketActor.props(
        EventsWebSocketActor.eventsAndAlerts(Some(102), powerPlantObservable),
        sink
      )
    )

    "produce telemetry signals every repeatable interval" in {
      telemetrySignalsWebSocketActor ! "Some Message"
    }

    "produce events and alerts" in {
      val transition = Transition(
        oldState = Init,
        newState = Active,
        powerPlantConfig = onOffTypeCfg,
        timeStamp = DateTime.now(DateTimeZone.UTC)
      )
      eventsWebSocketActor ! transition

      buffer foreach println
    }
  }
}

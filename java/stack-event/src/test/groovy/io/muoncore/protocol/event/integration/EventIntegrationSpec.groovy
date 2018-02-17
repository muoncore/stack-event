package io.muoncore.protocol.event.integration

import io.muoncore.MultiTransportMuon
import io.muoncore.Muon
import io.muoncore.codec.json.JsonOnlyCodecs
import io.muoncore.config.AutoConfiguration
import io.muoncore.liblib.reactor.rx.Streams
import io.muoncore.memory.discovery.InMemDiscovery
import io.muoncore.memory.transport.InMemTransport
import io.muoncore.memory.transport.bus.EventBus
import io.muoncore.protocol.Auth
import io.muoncore.protocol.event.ClientEvent
import io.muoncore.protocol.event.Event
import io.muoncore.protocol.event.client.DefaultEventClient
import io.muoncore.protocol.event.client.EventReplayMode
import io.muoncore.protocol.event.client.EventResult
import io.muoncore.protocol.event.server.EventServerProtocolStack
import io.muoncore.protocol.event.server.EventWrapper
import io.muoncore.protocol.reactivestream.messages.ReactiveStreamSubscriptionRequest
import io.muoncore.protocol.reactivestream.server.PublisherLookup
import io.muoncore.protocol.reactivestream.server.ReactiveStreamServer
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class EventIntegrationSpec extends Specification {

  def discovery = new InMemDiscovery()
  def eventbus = new EventBus()

  def "can emit a series of events and have them recieved on the server side"() {

    def data = []
    List<EventResult> results = []

    boolean fail = true

    def muon2 = muonEventStore { EventWrapper ev ->
      println "Event is the awesome ${ev.event}"
      data << ev.event
      if (!fail) {
        ev.persisted(12345, System.currentTimeMillis())
        fail = true
      } else {
        ev.failed("Something went wrong")
        fail = false
      }
    }

    def muon1 = muon("simples")

    def evClient = new DefaultEventClient(muon1)


    when:

    results << evClient.event(new ClientEvent("myid", "awesome", "SomethingHappened", "myid", "1234", "muon1", [msg: "HELLO WORLD"]), auth())
    results << evClient.event(new ClientEvent("myid", "awesome", "SomethingHappened", "myid", "1234", "muon1", [msg: "HELLO WORLD"]), auth())
    results << evClient.event(new ClientEvent("myid", "awesome", "SomethingHappened", "myid", "1234", "muon1", [msg: "HELLO WORLD"]), auth())
    results << evClient.event(new ClientEvent("myid", "awesome", "SomethingHappened", "myid", "1234", "muon1", [msg: "HELLO WORLD"]), auth())

    then:
    new PollingConditions().eventually {
      data.size() == 4
      results.size() == 4
      results.findAll { it.status == EventResult.EventResultStatus.PERSISTED }.size() == 2
      results.findAll { it.status == EventResult.EventResultStatus.FAILED }.size() == 2
    }
  }

  def "auth is available on the server"() {

    def capturedauth

    def muon2 = muonEventStore { EventWrapper ev ->
      capturedauth = ev.auth
    }

    def muon1 = muon("simples")

    def evClient = new DefaultEventClient(muon1)

    when:

    evClient.event(new ClientEvent("myid", "awesome", "SomethingHappened", "myid", "1234", "muon1", [msg: "HELLO WORLD"]), auth())

    then:
    new PollingConditions().eventually {
      capturedauth != null
      capturedauth == auth()
    }
  }

  def "data remains in order"() {

    def data = []

    def muon2 = muonEventStore { EventWrapper ev ->
      println "Event is the awesome ${ev.event}"
      data << ev.event
      ev.persisted(54321, System.currentTimeMillis())
    }

    def muon1 = muon("simples")
    def evClient = new DefaultEventClient(muon1)

    when:
    200.times {
      evClient.event(new ClientEvent("myid", "${it}", "SomethingHappened", "1.0", "1234", "muon1", [msg: "HELLO WORLD"]), auth())
    }

    then:
    new PollingConditions(timeout: 30).eventually {
      data.size() == 200
      def sorted = new ArrayList<Event>(data).sort {
        Integer.parseInt(it.eventType)
      }
      data == sorted
    }
  }

  def "partial replay works"() {

    def data = []

    def muon2 = muonEventStore { EventWrapper ev ->
      println "Event is the awesome ${ev.event}"
      data << ev.event
      ev.persisted(54321, System.currentTimeMillis())
    }

    def muon1 = muon("simples")
    def args
    muon2.publishGeneratedSource("/stream", PublisherLookup.PublisherType.HOT) {
      ReactiveStreamSubscriptionRequest subscriptionRequest ->
        println "ARGS = ${subscriptionRequest.args}"
        args = subscriptionRequest.args
        return Streams.from()
    }
    def evClient = new DefaultEventClient(muon1)
    def replayed = []

    when:
    evClient.replay("SomethingHappened", auth(),  EventReplayMode.REPLAY_THEN_LIVE, ["from": 112345], new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {
        s.request(Integer.MAX_VALUE)
      }

      @Override
      void onNext(Object o) {
        replayed << o
      }

      @Override
      void onError(Throwable t) { t.printStackTrace() }

      @Override
      void onComplete() {
        println "Completed"
      }
    })

    then:
    new PollingConditions(timeout: 30).eventually {
      args && args.from == "112345"
    }
  }

  Muon muon(name) {
    def config = new AutoConfiguration(serviceName: name)
    def transport = new InMemTransport(config, eventbus)

    new MultiTransportMuon(config, discovery, [transport], new JsonOnlyCodecs())
  }

  public ReactiveStreamServer muonEventStore(Closure handler) {
    def config = new AutoConfiguration(tags: ["eventstore"], serviceName: "chronos")
    def transport = new InMemTransport(config, eventbus)

    def muon = new MultiTransportMuon(config, discovery, [transport], new JsonOnlyCodecs())

    muon.protocolStacks.registerServerProtocol(new EventServerProtocolStack(handler, muon.codecs, discovery))

    new ReactiveStreamServer(muon)
  }

  Auth auth() {
    new Auth("faked", "faked")
  }
}

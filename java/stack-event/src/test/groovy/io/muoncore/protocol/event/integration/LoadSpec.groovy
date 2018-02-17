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
import io.muoncore.protocol.event.client.DefaultEventClient
import io.muoncore.protocol.event.client.EventReplayMode
import io.muoncore.protocol.event.server.EventServerProtocolStack
import io.muoncore.protocol.event.server.EventWrapper
import io.muoncore.protocol.reactivestream.messages.ReactiveStreamSubscriptionRequest
import io.muoncore.protocol.reactivestream.server.PublisherLookup
import io.muoncore.protocol.reactivestream.server.ReactiveStreamServer
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class LoadSpec extends Specification {

    def discovery = new InMemDiscovery()
    def eventbus = new EventBus()

  def "many subscribes and then emit works"() {

    def data = []

    def muon2 = muonEventStore { EventWrapper ev ->
      println "Event is the awesome ${ev.event}"
      data << ev.event
      ev.persisted(54321, System.currentTimeMillis())
    }

    def muons = [
      muon("simple1"),
      muon("simple2"),
      muon("simple3"),
      muon("simple4"),
      muon("simple5"),
      muon("simple6")]

    def args
    muon2.publishGeneratedSource("/stream", PublisherLookup.PublisherType.HOT) {
      ReactiveStreamSubscriptionRequest subscriptionRequest ->
        println "ARGS = ${subscriptionRequest.args}"
        args = subscriptionRequest.args
        return Streams.from()
    }

    def evClients = muons.collect {
      new DefaultEventClient(it)
    }

    when:
    def replayed = evClients.collect {
      def replayed = []

      it.replay("SomethingHappened",auth(),  EventReplayMode.REPLAY_THEN_LIVE, ["from": 112345], new Subscriber() {
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
      replayed
    }

    50.times {
      evClients[0].event(new ClientEvent("id", "${it}" as String, "SomethingHappened", "1.0", "", "muon1", [msg: "HELLO WORLD"]), auth())
    }

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

    ReactiveStreamServer muonEventStore(Closure handler) {
        def config = new AutoConfiguration(tags:["eventstore"], serviceName: "chronos")
        def transport = new InMemTransport(config, eventbus)

        def muon = new MultiTransportMuon(config, discovery, [transport], new JsonOnlyCodecs())

        muon.protocolStacks.registerServerProtocol(new EventServerProtocolStack(handler, muon.codecs, discovery))

        new ReactiveStreamServer(muon)
    }

  Auth auth() {
    new Auth("faked", "faked")
  }
}

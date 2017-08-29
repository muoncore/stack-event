package io.muoncore.protocol.event.client

import io.muoncore.Discovery
import io.muoncore.Muon
import io.muoncore.ServiceDescriptor
import io.muoncore.api.MuonFuture
import io.muoncore.channel.ChannelConnection
import io.muoncore.channel.impl.StandardAsyncChannel
import io.muoncore.channel.support.Scheduler
import io.muoncore.codec.Codecs
import io.muoncore.config.AutoConfiguration
import io.muoncore.descriptors.ProtocolDescriptor
import io.muoncore.descriptors.ServiceExtendedDescriptor
import io.muoncore.message.MuonMessage
import io.muoncore.message.MuonMessageBuilder
import io.muoncore.protocol.ChannelFunctionExecShimBecauseGroovyCantCallLambda
import io.muoncore.protocol.event.ClientEvent
import io.muoncore.transport.client.TransportClient
import spock.lang.Specification
import spock.lang.Timeout
import spock.util.concurrent.PollingConditions

@Timeout(5)
class EventClientProtocolStackSpec extends Specification {

    def "Stack sends all with the event protocol set"() {

        StandardAsyncChannel.echoOut=true
        def config = new AutoConfiguration(serviceName: "tombola")

        def discovery = Mock(Discovery) {
          getServiceWithTags(_) >> Optional.of(new ServiceDescriptor("tombola", [], [], [], []))
        }

        def clientChannel = Mock(ChannelConnection)
        def transportClient = Mock(TransportClient) {
            openClientChannel() >> clientChannel
        }

        def muon = Mock(Muon) {
            getTransportClient() >> transportClient
            getDiscovery() >> discovery
            getConfiguration() >> config
            getCodecs() >> Mock(Codecs) {
                getAvailableCodecs() >> ([] as String[])
                encode(_, _) >> new Codecs.EncodingResult(new byte[0], "application/json")
            }
            getScheduler() >> new Scheduler()
        }

        def eventStore = new DefaultEventClient(muon)

        when:
        eventStore.event(
                ClientEvent.ofType("SomethingHappened").stream("awesome").payload([]).build())
        sleep(50)

        then:
        1 * clientChannel.send({ it?.protocol == "event" })

        cleanup:
        StandardAsyncChannel.echoOut=false
    }

    def "Sends a 404 response if no eventstore service found"() {

        StandardAsyncChannel.echoOut=true

        def discovery = Mock(Discovery) {
          getServiceWithTags(_) >> Optional.empty()
        }
        def clientChannel = Mock(ChannelConnection)
        def transportClient = Mock(TransportClient) {
            openClientChannel() >> clientChannel
        }

        def muon = Mock(Muon) {
            getTransportClient() >> transportClient
            getDiscovery() >> discovery
        }

        def eventStore = new DefaultEventClient(muon)

        when:
        def response = eventStore.event(
                new ClientEvent("myid", "awesome", "SomethingHappened2", "simples", "1234", "myService", []))

        then:
        response
        response.status == EventResult.EventResultStatus.FAILED

        cleanup:
        StandardAsyncChannel.echoOut=false
    }

    @Timeout(30)
    def "Sends a failed response if timeout occurs"() {

        StandardAsyncChannel.echoOut=true
        def config = new AutoConfiguration(serviceName: "tombola")
        def discovery = Mock(Discovery) {
          getServiceWithTags(_) >> Optional.of(new ServiceDescriptor("tombola", [], [], [], []))
        }
        def clientChannel = Mock(ChannelConnection)
        def transportClient = Mock(TransportClient) {
            openClientChannel() >> clientChannel
        }

        def muon = Mock(Muon) {
            getScheduler() >> new Scheduler()
            getConfiguration() >> config
            getTransportClient() >> transportClient
            getDiscovery() >> discovery
            getCodecs() >> Mock(Codecs) {
                getAvailableCodecs() >> ([] as String[])
                encode(_, _) >> new Codecs.EncodingResult(new byte[0], "application/json")
            }
        }

        def eventStore = new DefaultEventClient(muon)

        when:
        def response = eventStore.event(
                new ClientEvent("myid", "awesome", "SomethingHappened2", "simples", "1234", "myService", []))

        then:
        response
        response.status == EventResult.EventResultStatus.FAILED

        cleanup:
        StandardAsyncChannel.echoOut=false
    }

    def muon() {
        Muon
    }
}

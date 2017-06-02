package io.muoncore.protocol.event.client

import io.muoncore.Discovery
import io.muoncore.ServiceDescriptor
import io.muoncore.channel.Channel
import io.muoncore.channel.Channels
import io.muoncore.channel.impl.TimeoutChannel
import io.muoncore.codec.Codecs
import io.muoncore.config.AutoConfiguration
import io.muoncore.message.MuonInboundMessage
import io.muoncore.message.MuonMessageBuilder
import io.muoncore.message.MuonOutboundMessage
import io.muoncore.protocol.event.ClientEvent
import io.muoncore.protocol.event.EventProtocolMessages
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class EventClientProtocolSpec extends Specification {

    def "protocol sends an event on for event Event"() {

        def discovery = Mock(Discovery) {
          getServiceWithTags(_) >> Optional.of(new ServiceDescriptor("tombola", [], [], [], []))
        }
        def codecs = Mock(Codecs) {
            encode(_, _) >> new Codecs.EncodingResult(null, null)
            getAvailableCodecs() >> []
        }

        def leftChannel = Channels.channel("left", "right")
        def rightChannel = Channels.channel("left", "right")

        def ret

        rightChannel.right().receive({
            ret = it
        })

        def proto = new EventClientProtocol(
                new AutoConfiguration(serviceName: "tombola"),
                discovery, codecs,
                leftChannel.right(), rightChannel.left())

        when:
        leftChannel.left().send(ClientEvent.ofType("awesome")
                .stream("awesome")
                .payload(["1":2, "payload":true])
                .build())

        then:
        new PollingConditions().eventually {
            ret instanceof MuonOutboundMessage
            ret.step == EventProtocolMessages.EVENT
        }
    }

    def "protocol returns FAILED if a timeout given"() {

        def discovery = Mock(Discovery) {
          getServiceWithTags(_) >> Optional.of(new ServiceDescriptor("tombola", [], [], [], []))
        }
        def codecs = Mock(Codecs) {
            encode(_, _) >> new Codecs.EncodingResult(null, null)
            getAvailableCodecs() >> []
        }

        def leftChannel = Channels.channel("left", "right")
        Channel<MuonOutboundMessage, MuonInboundMessage> rightChannel = Channels.channel("left", "right")

        def ret

        rightChannel.right().receive({
            rightChannel.right().send(MuonMessageBuilder.fromService("blah")
                                            .step(TimeoutChannel.TIMEOUT_STEP)
                                            .buildInbound())
        })

        leftChannel.left().receive {
            if (it != null) {
                println "Got a response! ${it}"
                ret = it
            }
        }

        def proto = new EventClientProtocol(
                new AutoConfiguration(serviceName: "tombola"),
                discovery, codecs,
                leftChannel.right(), rightChannel.left())

        when:
        leftChannel.left().send(ClientEvent.ofType("awesome")
                .stream("awesome")
                .payload(["1":2, "payload":true])
                .build())

        then:
        new PollingConditions().eventually {
            ret instanceof EventResult
            ret.status == EventResult.EventResultStatus.FAILED
        }
    }
}

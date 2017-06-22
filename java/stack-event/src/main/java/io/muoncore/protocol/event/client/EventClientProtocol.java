package io.muoncore.protocol.event.client;

import io.muoncore.Discovery;
import io.muoncore.ServiceDescriptor;
import io.muoncore.channel.ChannelConnection;
import io.muoncore.channel.impl.TimeoutChannel;
import io.muoncore.codec.Codecs;
import io.muoncore.config.AutoConfiguration;
import io.muoncore.message.MuonInboundMessage;
import io.muoncore.message.MuonMessageBuilder;
import io.muoncore.message.MuonOutboundMessage;
import io.muoncore.protocol.event.ClientEvent;
import io.muoncore.protocol.event.EventCodec;
import io.muoncore.protocol.event.EventProtocolMessages;
import io.muoncore.transport.TransportEvents;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * This middleware will accept an Event. It will then attempt to locate an event store to send a persistence Request to
 */
@Slf4j
public class EventClientProtocol<X> {

  public EventClientProtocol(
    AutoConfiguration configuration,
    Discovery discovery,
    Codecs codecs,
    ChannelConnection<EventResult, ClientEvent> leftChannelConnection,
    ChannelConnection<MuonOutboundMessage, MuonInboundMessage> rightChannelConnection) {

    rightChannelConnection.receive(message -> {
      if (message == null) {
        leftChannelConnection.shutdown();
        return;
      }

      EventResult result;

      switch (message.getStep()) {
        case EventProtocolMessages.EVENT_RESULT:
          result = codecs.decode(message.getPayload(), message.getContentType(), EventResult.class);
          break;
        case TransportEvents.SERVICE_NOT_FOUND:
          result = new EventResult(EventResult.EventResultStatus.FAILED,
            "Event Store Service Not Found");
          break;
        case TransportEvents.CONNECTION_FAILURE:
          result = new EventResult(EventResult.EventResultStatus.FAILED,
            "Event Store is not contactable, the transport could not complete a connection to it. This may be transient");
          break;
        case TransportEvents.PROTOCOL_NOT_FOUND:
          result = new EventResult(EventResult.EventResultStatus.FAILED,
            "Remote service does not support event sink protocol");
          break;

        case TimeoutChannel.TIMEOUT_STEP:
          result = new EventResult(EventResult.EventResultStatus.FAILED,
            "A timeout occurred, the remote service did not send a response");
          break;
        default:
          if ("text/plain".equals(message.getContentType())) {
            log.debug("Unknown step '{}'", message.getStep());
            result = new EventResult(EventResult.EventResultStatus.FAILED, new String(message.getPayload()));
          } else {
            result = new EventResult(EventResult.EventResultStatus.FAILED, "Unknown step " + message.getStep());
          }
      }

      leftChannelConnection.send(result);
      leftChannelConnection.shutdown();
    });

    leftChannelConnection.receive(event -> {
      if (event == null) {
        rightChannelConnection.shutdown();
        return;
      }
      Optional<ServiceDescriptor> eventService = discovery.getServiceWithTags("eventstore");

      if (!eventService.isPresent()) {
        //TODO, a failure, no event store available.
        leftChannelConnection.send(new EventResult(EventResult.EventResultStatus.FAILED,
          "No Event Store available"));
      } else {

        Map<String, Object> payload = EventCodec.getMapFromClientEvent(event, configuration);

        Codecs.EncodingResult result = codecs.encode(payload, eventService.get().getCodecs());
        MuonOutboundMessage msg = MuonMessageBuilder.fromService(configuration.getServiceName())
          .toService(eventService.get().getIdentifier())
          .protocol(EventProtocolMessages.PROTOCOL)
          .step(EventProtocolMessages.EVENT)
          .contentType(result.getContentType())
          .payload(result.getPayload()).build();

        rightChannelConnection.send(msg);
      }
    });
  }
}

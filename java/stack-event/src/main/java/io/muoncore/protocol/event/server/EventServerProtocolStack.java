package io.muoncore.protocol.event.server;

import io.muoncore.Discovery;
import io.muoncore.channel.*;
import io.muoncore.codec.Codecs;
import io.muoncore.descriptors.ProtocolDescriptor;
import io.muoncore.descriptors.SchemaDescriptor;
import io.muoncore.message.MuonInboundMessage;
import io.muoncore.message.MuonMessageBuilder;
import io.muoncore.message.MuonOutboundMessage;
import io.muoncore.protocol.Auth;
import io.muoncore.protocol.ServerProtocolStack;
import io.muoncore.protocol.event.Event;
import io.muoncore.protocol.event.EventCodec;
import io.muoncore.protocol.event.EventProtocolMessages;
import io.muoncore.protocol.event.client.EventResult;

import java.util.Collections;
import java.util.Map;

/**
 * Server side of the event protocol
 */
public class EventServerProtocolStack implements
        ServerProtocolStack {

    private final ChannelConnection.ChannelFunction<EventWrapper>handler;
    private Codecs codecs;
    private Discovery discovery;
    private Dispatcher dispatcher = Dispatchers.poolDispatcher();

    public EventServerProtocolStack(ChannelConnection.ChannelFunction<EventWrapper> handler,
                                    Codecs codecs, Discovery discovery) {
        this.codecs = codecs;
        this.handler = handler;
        this.discovery = discovery;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelConnection<MuonInboundMessage, MuonOutboundMessage> createChannel() {

        Channel<MuonOutboundMessage, MuonInboundMessage> api2 = Channels.channel("eventserver", "transport");

        api2.left().receive( message -> {
            if (message == null || message.getPayload() == null) {
                return;
            }

            Map data = codecs.decode(message.getPayload(), message.getContentType(), Map.class);
            Event ev = EventCodec.getEventFromMap(data, codecs);
            Auth auth = EventCodec.getAuthFromMap(data, codecs);

            Channel<EventResult, EventWrapper> evserver = Channels.channel("eventserverapp", "wrapper");
            EventWrapper wrapper = new EventWrapper(ev, auth, evserver.left());

            evserver.right().receive( eventResult -> {

                Codecs.EncodingResult result = codecs.encode(eventResult, discovery.getCodecsForService(message.getSourceServiceName()));

                MuonOutboundMessage msg = MuonMessageBuilder.fromService(message.getTargetServiceName())
                        .toService(message.getSourceServiceName())
                        .protocol(EventProtocolMessages.PROTOCOL)
                        .step(EventProtocolMessages.EVENT_RESULT)
                        .contentType(result.getContentType())
                        .payload(result.getPayload()).build();

                dispatcher.dispatch(msg, api2.left()::send, Throwable::printStackTrace);
            });

            dispatcher.dispatch(wrapper, handler::apply, Throwable::printStackTrace);
        });

        return api2.right();
    }

    @Override
    public ProtocolDescriptor getProtocolDescriptor() {

        return new ProtocolDescriptor(
                EventProtocolMessages.PROTOCOL,
                "Event Sink Protocol",
                "Provides a discoverable sink for events to flow into without needing explicit service endpoints",
                Collections.emptyList());
    }

  @Override
  public Map<String, SchemaDescriptor> getSchemasFor(String endpoint) {
    return Collections.emptyMap();
  }
}

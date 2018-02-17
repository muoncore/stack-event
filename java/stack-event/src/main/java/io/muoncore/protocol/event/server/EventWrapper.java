package io.muoncore.protocol.event.server;

import io.muoncore.channel.ChannelConnection;
import io.muoncore.protocol.Auth;
import io.muoncore.protocol.event.Event;
import io.muoncore.protocol.event.client.EventResult;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventWrapper {

    private Event event;
    private Auth auth;
    private ChannelConnection<EventResult, ?> channel;

    public void persisted(
            long orderId,
            long eventTime
    ) {
        channel.send(new EventResult(
                EventResult.EventResultStatus.PERSISTED, "Event persisted", orderId, eventTime
        ));
    }

    public void failed(String reason) {
        channel.send(new EventResult(
                EventResult.EventResultStatus.FAILED, reason
        ));
    }
}

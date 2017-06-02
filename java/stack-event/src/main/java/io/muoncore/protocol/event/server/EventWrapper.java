package io.muoncore.protocol.event.server;

import io.muoncore.channel.ChannelConnection;
import io.muoncore.protocol.event.Event;
import io.muoncore.protocol.event.client.EventResult;

public class EventWrapper {

    private Event event;
    private ChannelConnection<EventResult, ?> channel;

    public EventWrapper(Event event, ChannelConnection<EventResult, ?> channel) {
        this.event = event;
        this.channel = channel;
    }

    public Event getEvent() {
        return event;
    }

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

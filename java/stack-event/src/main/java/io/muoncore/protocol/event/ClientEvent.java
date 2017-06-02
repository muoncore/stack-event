package io.muoncore.protocol.event;

/**
 * An event created by a client, ready to be passed to an event store for persistence and cononicalisation
 */
public class ClientEvent {


    private String eventType;
    private String streamName;
    private Object payload;

    private Long causedById;
    private String causedByRelation;
    private String schema;

    public ClientEvent(
            String eventType,
            String streamName,
            String schema,
            Long causedById,
            String causedByRelation,
            Object payload) {
        this.schema = schema;
        this.eventType = eventType;
        this.streamName = streamName;
        this.payload = payload;
        this.causedById = causedById;
        this.causedByRelation = causedByRelation;
    }

    public String getSchema() {
        return schema;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStreamName() {
        return streamName;
    }

    public Object getPayload() {
        return payload;
    }

    public Long getCausedById() {
        return causedById;
    }

    public String getCausedByRelation() {
        return causedByRelation;
    }

    @Override
    public String toString() {
        return "ClientEvent{" +
                "eventType='" + eventType + '\'' +
                ", streamName='" + streamName + '\'' +
                ", causedById=" + causedById +
                ", causedByRelation='" + causedByRelation + '\'' +
                ", schema='" + schema + '\'' +
                '}';
    }

    public static EventBuilder ofType(String type) {
        return new EventBuilder().eventType(type);
    }

}

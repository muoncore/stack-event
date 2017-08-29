package io.muoncore.protocol.event;

import lombok.Getter;
import lombok.ToString;

/**
 * An event created by a client, ready to be passed to an event store for persistence and canonicalisation
 */
@ToString
public class ClientEvent {

  @Getter
  private String id;
  private String eventType;
  private String streamName;
  private Object payload;

  private String causedById;
  private String causedByRelation;
  private String schema;

  public ClientEvent(
    String id,
    String eventType,
    String streamName,
    String schema,
    String causedById,
    String causedByRelation,
    Object payload) {
    this.id = id;
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

  public String getCausedById() {
    return causedById;
  }

  public String getCausedByRelation() {
    return causedByRelation;
  }

  public static EventBuilder ofType(String type) {
    return new EventBuilder().eventType(type);
  }

}

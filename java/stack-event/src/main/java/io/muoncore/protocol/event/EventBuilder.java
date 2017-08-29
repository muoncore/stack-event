package io.muoncore.protocol.event;

/**
 * Construct a ClientEvent using a fluent builder.
 */
public class EventBuilder {
  private String id;
  private Object payload;
  private String eventType;
  private String streamName = "default";

  private String causedById;
  private String causedByRelation;
  private String schema;

  /**
   * The payload of this event. can be any object that can be serialised using an existing codec.
   * <p>
   * Optional.
   */
  public EventBuilder payload(Object payload) {
    this.payload = payload;
    return this;
  }

  /**
   * Set the schema version of this event. This can be used later on during projection or replay
   * to enable auto upgrade of the event schema during replay and projection.
   * <p>
   * Optional
   */
  public EventBuilder schema(String schema) {
    this.schema = schema;
    return this;
  }

  /**
   * The event type. This gives the event an identity
   * <p>
   * Mandatory.
   */
  public EventBuilder eventType(String eventType) {
    this.eventType = eventType;
    return this;
  }

  public EventBuilder id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Which stream this event should persist on.
   * <p>
   * if not supplied, will be set to 'default'
   */
  public EventBuilder stream(String streamName) {
    this.streamName = streamName;
    return this;
  }

  /**
   * Add a causal relationship to another event.
   * <p>
   * Optional
   */
  public EventBuilder causedBy(String causedById, String relation) {
    this.causedById = causedById;
    this.causedByRelation = relation;
    return this;
  }

  public ClientEvent build() {
    return new ClientEvent(
      id,
      eventType,
      streamName,
      schema,
      causedById,
      causedByRelation,
      payload);
  }
}

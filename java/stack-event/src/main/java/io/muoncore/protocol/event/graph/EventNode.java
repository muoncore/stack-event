package io.muoncore.protocol.event.graph;

import java.util.List;

public class EventNode {
    private String id;
    private String serviceId;
    private String eventType;

    private EventNode parent;
    private List<EventNode> children;

    public EventNode(String id, String serviceId, String eventType, EventNode parent, List<EventNode> children) {
        this.id = id;
        this.parent= parent;
        this.serviceId = serviceId;
        this.eventType = eventType;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getEventType() {
        return eventType;
    }

    public EventNode getParent() {
        return parent;
    }

    public List<EventNode> getChildren() {
        return children;
    }
}

package io.muoncore.protocol.event.client;

public class EventResult {

    private long eventTime;
    private long orderId;
    private EventResultStatus status;
    private String cause;

    public EventResult() {
    }

    public EventResult(EventResultStatus status, String cause) {
        this.status = status;
        this.cause = cause;
    }

    public EventResult(EventResultStatus status, String cause, long orderId, long eventTime) {
        this.status = status;
        this.cause = cause;
        this.orderId = orderId;
        this.eventTime = eventTime;
    }

    public long getEventTime() {
        return eventTime;
    }

    public long getOrderId() {
        return orderId;
    }

    public EventResultStatus getStatus() {
        return status;
    }

    public String getCause() {
        return cause;
    }

    public enum EventResultStatus {
        PERSISTED, FAILED
    }

    @Override
    public String toString() {
        return "EventResult{" +
                "eventTime=" + eventTime +
                ", orderId=" + orderId +
                ", status=" + status +
                ", cause='" + cause + '\'' +
                '}';
    }
}


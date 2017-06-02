package io.muoncore.protocol.event.client;

public class EventProjectionDescriptor {

    private String name;

    public EventProjectionDescriptor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "EventProjectionDescriptor{" +
                "name='" + name + '\'' +
                '}';
    }
}


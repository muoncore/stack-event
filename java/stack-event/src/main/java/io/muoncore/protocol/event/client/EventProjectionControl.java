package io.muoncore.protocol.event.client;

public interface EventProjectionControl<ProjectionType> {

    /**
     * Obtain the latest available projection state.
     */
    ProjectionType getCurrentState();


}


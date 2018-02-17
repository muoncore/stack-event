package io.muoncore.protocol.event.client;

import io.muoncore.api.MuonFuture;
import io.muoncore.protocol.Auth;
import io.muoncore.protocol.event.ClientEvent;
import io.muoncore.protocol.event.Event;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Map;

public interface EventClient {

    /**
     * Emit an event into the remote event store.
     * @param event
     * @return
     */
    EventResult event(ClientEvent event, Auth auth);
    MuonFuture<EventResult> eventAsync(ClientEvent event, Auth auth);

    /**
     * Load an event by id
     */
//    <X> MuonFuture<Event<X>> loadEvent(String id, Class<X> type);

    /**
     * Replay an event stream, allowing the creation of an aggregated data structure (a reduction or projection)
     * Or serial processing of the stream.
     *
     * This method requires an event store to be active in the distributed system. If one is not active, a MuonException
     * will be thrown.
     *
     * This will optionally replay from the start of the stream up to the current and then switch to HOT processing for all messages
     * after this.
     *
     * @param streamName The name of the stream to be replayed
     * @param mode Whether to replay just the future data, or request to load historical data, if supported on the remote stream
     * @param subscriber The reactive streams subscriber that will listen to the event stream.
     */
    <X> MuonFuture<EventReplayControl> replay(String streamName, Auth auth, EventReplayMode mode, Subscriber<Event> subscriber);
    <X> MuonFuture<EventReplayControl> replay(String streamName, Auth auth, EventReplayMode mode, Map<String, Object> args, Subscriber<Event> subscriber);
}

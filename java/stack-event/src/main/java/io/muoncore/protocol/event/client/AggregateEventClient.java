package io.muoncore.protocol.event.client;

import io.muoncore.api.MuonFuture;
import io.muoncore.exception.MuonException;
import io.muoncore.protocol.event.ClientEvent;
import io.muoncore.protocol.event.Event;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AggregateEventClient {

  private EventClient client;

  public AggregateEventClient(EventClient client) {
    this.client = client;
  }

  /**
   * Publish the given events to the provided aggregate event stream
   *
   * The ID should be for the aggregate root
   *
   * The events will be converted into Muon `Event` types
   * * stream will be /aggregate/[id]
   * * type will be the event type class Simple Name - eg co.myapp.UserCreatedEvent -> UserCreatedEvent
   *
   * @param id
   * @param events
   */
  public void publishDomainEvents(String id, List events) {
    events.forEach(domainEvent -> {
      ClientEvent persistEvent = ClientEvent
        .ofType(domainEvent.getClass().getSimpleName())
        .payload(domainEvent)
        .stream("/aggregate/" + id)
        .build();

      EventResult result = client.event(persistEvent);

      if (result.getStatus() == EventResult.EventResultStatus.FAILED) {
        throw new MuonException("Failed to persist domain event " + domainEvent + ":" + result.getCause());
      }
    });
  }

  public List<Event> loadAggregateRoot(String id) throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    List<Event> events = new ArrayList<>();

    client.replay("/aggregate/" + id, EventReplayMode.REPLAY_ONLY, new Subscriber<Event>() {
      @Override
      public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(Event o) {
        events.add(o);
      }

      @Override
      public void onError(Throwable t) {
        latch.countDown();
      }

      @Override
      public void onComplete() {
        latch.countDown();
      }
    });

    latch.await();

    return events;
  }
}

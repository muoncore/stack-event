package io.muoncore.protocol.event.client;

import io.muoncore.Discovery;
import io.muoncore.Muon;
import io.muoncore.ServiceDescriptor;
import io.muoncore.api.ChannelFutureAdapter;
import io.muoncore.api.MuonFuture;
import io.muoncore.channel.Channel;
import io.muoncore.channel.Channels;
import io.muoncore.codec.Codecs;
import io.muoncore.config.AutoConfiguration;
import io.muoncore.exception.MuonException;
import io.muoncore.message.MuonInboundMessage;
import io.muoncore.message.MuonOutboundMessage;
import io.muoncore.protocol.Auth;
import io.muoncore.protocol.event.ClientEvent;
import io.muoncore.protocol.event.Event;
import io.muoncore.protocol.reactivestream.client.ReactiveStreamClient;
import io.muoncore.protocol.reactivestream.client.StreamData;
import io.muoncore.transport.client.TransportClient;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DefaultEventClient implements EventClient {

  private AutoConfiguration config;
  private Discovery discovery;
  private Codecs codecs;
  private TransportClient transportClient;
  private ReactiveStreamClient reactiveStreamClientProtocolStack;

  private Muon muon;

  public DefaultEventClient(Muon muon) {
    this.muon = muon;
    this.config = muon.getConfiguration();
    this.discovery = muon.getDiscovery();
    this.codecs = muon.getCodecs();
    this.transportClient = muon.getTransportClient();
    this.reactiveStreamClientProtocolStack = new ReactiveStreamClient(muon);
  }

  @Override
  public MuonFuture<EventResult> eventAsync(ClientEvent event, Auth auth) {
    Channel<ClientEvent, EventResult> api2eventproto = Channels.channel("eventapi", "eventproto");

    ChannelFutureAdapter<EventResult, ClientEvent> adapter =
      new ChannelFutureAdapter<>(api2eventproto.left());

    Channel<MuonOutboundMessage, MuonInboundMessage> timeoutChannel = Channels.timeout(muon.getScheduler(), 1000);

    new EventClientProtocol<>(
      config,
      discovery,
      codecs,
      auth,
      api2eventproto.right(),
      timeoutChannel.left());

    Channels.connect(timeoutChannel.right(), transportClient.openClientChannel());

    return adapter.request(event);
  }


  @Override
  public EventResult event(ClientEvent event, Auth auth) {
    try {
      Channel<ClientEvent, EventResult> api2eventproto = Channels.channel("eventapi", "eventproto");

      ChannelFutureAdapter<EventResult, ClientEvent> adapter =
        new ChannelFutureAdapter<>(api2eventproto.left());

      Channel<MuonOutboundMessage, MuonInboundMessage> timeoutChannel = Channels.timeout(muon.getScheduler(), 1000);

      new EventClientProtocol<>(
        config,
        discovery,
        codecs,
        auth,
        api2eventproto.right(),
        timeoutChannel.left());

      Channels.connect(timeoutChannel.right(), transportClient.openClientChannel());

      return adapter.request(event).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new MuonException(e);
    }
  }

  @Override
  public <X> MuonFuture<EventReplayControl> replay(String streamName, Auth auth, EventReplayMode mode, Subscriber<Event> subscriber) {
    return replay(streamName, auth, mode, Collections.emptyMap(), subscriber);
  }

  @Override
  public <X> MuonFuture<EventReplayControl> replay(String streamName, Auth auth, EventReplayMode mode, Map<String, Object> args, Subscriber<Event> subscriber) {
    String replayType;
    switch (mode) {
      case LIVE_ONLY:
        replayType = "hot";
        break;
      case REPLAY_ONLY:
        replayType = "cold";
        break;
      case REPLAY_THEN_LIVE:
      default:
        replayType = "hot-cold";
    }

    //TODO, manage a params object and turn it into a querystring.
    Map<String, Object> params = new HashMap<>();
    params.put("stream-type", replayType);
    params.put("stream-name", streamName);

    params.putAll(args);

    String query = params.entrySet()
      .stream()
      .map(entry -> entry.getKey() + "=" + entry.getValue())
      .collect(Collectors.joining("&"));


    Optional<ServiceDescriptor> eventStore = discovery.getServiceWithTags("eventstore");
    if (eventStore.isPresent()) {
      String eventStoreName = eventStore.get().getIdentifier();
      try {
        reactiveStreamClientProtocolStack.subscribe(new URI("stream://" + eventStoreName + "/stream?" + query), new Subscriber<StreamData>() {
          @Override
          public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(s);
          }

          @Override
          public void onNext(StreamData data) {
            Event event = data.getPayload(Event.class);
            event.setCodecs(codecs);
            subscriber.onNext(event);
          }

          @Override
          public void onError(Throwable t) {
            subscriber.onError(t);
          }

          @Override
          public void onComplete() {
            subscriber.onComplete();
          }
        });
      } catch (URISyntaxException e) {
        throw new MuonException("The name provided [" + eventStoreName + "] is invalid", e);
      }
    } else {
      throw new MuonException("There is no event store present in the distributed system, is Photon running?");
    }
    return null;
  }
}

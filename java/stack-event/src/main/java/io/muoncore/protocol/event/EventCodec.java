package io.muoncore.protocol.event;

import io.muoncore.codec.Codecs;
import io.muoncore.config.AutoConfiguration;

import java.util.HashMap;
import java.util.Map;

public class EventCodec {

    private static final String ID = "id";
    private static final String STREAM_NAME = "stream-name";
    private static final String PAYLOAD = "payload";
    private static final String EVENT_TYPE = "event-type";
    private static final String CAUSED_BY = "caused-by";
    private static final String CAUSED_BY_RELATION = "caused-by-relation";
    private static final String SERVICE = "service-id";
    private static final String ORDER_ID = "order-id";
    private static final String EVENT_TIME = "event-time";
    private static final String SCHEMA = "schema";


    public static Event getEventFromMap(Map<String, Object> data, Codecs codecs) {
        return new Event(
          (String) data.get(ID),
                (String) data.get(EVENT_TYPE),
                (String) data.get(STREAM_NAME),
                (String) data.get(SCHEMA),
                getCausedByIdAsLong(data.get(CAUSED_BY)),
                (String) data.get(CAUSED_BY_RELATION),
                (String) data.get(SERVICE),
                (Long)   data.get(ORDER_ID),
                (Long)   data.get(EVENT_TIME),
                (Map)data.get(PAYLOAD),
                codecs
        );
    }

    private static String getCausedByIdAsLong(Object val) {
        if (val instanceof Double) {
            Double dat = (Double) val;
            return String.valueOf(dat.longValue());
        }
        return String.valueOf(val);
    }

    public static Map<String, Object> getMapFromClientEvent(ClientEvent event, AutoConfiguration config) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(ID, event.getId());
        payload.put(STREAM_NAME, event.getStreamName());
        payload.put(PAYLOAD, event.getPayload());
        payload.put(EVENT_TYPE, event.getEventType());
        payload.put(CAUSED_BY, event.getCausedById());
        payload.put(CAUSED_BY_RELATION, event.getCausedByRelation());
        payload.put(SERVICE, config.getServiceName());
        payload.put(SCHEMA, event.getSchema());
        return payload;
    }

    public static Map<String, Object> getMapFromEvent(Event event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(ID, event.getId());
        payload.put(STREAM_NAME, event.getStreamName());
        payload.put(PAYLOAD, event.getPayload(Map.class));
        payload.put(EVENT_TYPE, event.getEventType());
        payload.put(CAUSED_BY, event.getCausedById());
        payload.put(CAUSED_BY_RELATION, event.getCausedByRelation());
        payload.put(SERVICE, event.getService());
        payload.put(ORDER_ID, event.getOrderId());
        payload.put(EVENT_TIME, event.getEventTime());
        payload.put(SCHEMA, event.getSchema());
        return payload;
    }
}

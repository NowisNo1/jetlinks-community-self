package org.jetlinks.community.network.http.executor;

import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class HttpMessageCodec implements RuleDataCodec<HttpMessage> {

    private static final HttpMessageCodec instance = new HttpMessageCodec();

    static {
        RuleDataCodecs.register(HttpMessage.class, instance);
    }

    static void register() {
    }

    @Override
    public Object encode(HttpMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
                .orElse(PayloadType.BINARY);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("payloadType", payloadType.name());

        return map;
    }

    @Override
    public Flux<HttpMessage> decode(RuleData data, Feature... features) {
        return data
                .dataToMap()
                .flatMap(map -> {
                    Object payload = map.get("payload");
                    if (payload == null) {
                        return Mono.empty();
                    }
                    PayloadType payloadType = Feature
                            .find(PayloadType.class, features)
                            .orElse(PayloadType.BINARY);

                    HttpMessage message = new HttpMessage();
                    message.setPayload(payloadType.write(payload));
                    //message.setPayloadType(MessagePayloadType.valueOf(payloadType.name()));

                    return Mono.just(message);
                });
    }
}

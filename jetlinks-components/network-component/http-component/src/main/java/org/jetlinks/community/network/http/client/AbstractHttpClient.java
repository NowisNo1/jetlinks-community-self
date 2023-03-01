package org.jetlinks.community.network.http.client;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.http.HttpMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Slf4j
public abstract class AbstractHttpClient implements HttpClient {
    private EmitterProcessor<HttpMessage> processor = EmitterProcessor.create(false);

    protected void received(HttpMessage message) {
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("not handler,drop http message:{}", message.getPayload().toString(StandardCharsets.UTF_8));
            return;
        }
        processor.onNext(message);
    }

    @Override
    public Flux<HttpMessage> subscribe() {
        return processor
            .map(Function.identity());
    }
}

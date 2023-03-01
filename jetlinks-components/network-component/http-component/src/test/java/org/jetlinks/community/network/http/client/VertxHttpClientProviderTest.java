package org.jetlinks.community.network.http.client;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.community.network.http.parser.DefaultPayloadParserBuilder1;
import org.jetlinks.community.network.http.parser.PayloadParserType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

class VertxHttpClientProviderTest {


    @Test
    void test() {
        Vertx vertx = Vertx.vertx();

        vertx.createNetServer()
                .connectHandler(socket -> {
                    socket.write("tes");
                    socket.write("ttest");
                })
                .listen(12311);

        VertxHttpClientProvider provider = new VertxHttpClientProvider(id -> Mono.empty(), vertx, new DefaultPayloadParserBuilder1());

        HttpClientProperties properties = new HttpClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(12311);
        properties.setParserType(PayloadParserType.FIXED_LENGTH);
        properties.setParserConfiguration(Collections.singletonMap("size", 4));
        properties.setOptions(new NetClientOptions());


        provider.createNetwork(properties)
                .subscribe()
                .map(HttpMessage::getPayload)
                .map(buf -> buf.toString(StandardCharsets.UTF_8))
                .take(3)
                .as(StepVerifier::create)
                .expectNext("test", "test")
                .verifyComplete();

    }

}
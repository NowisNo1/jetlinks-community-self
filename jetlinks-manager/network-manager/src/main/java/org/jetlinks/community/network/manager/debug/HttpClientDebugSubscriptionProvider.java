package org.jetlinks.community.network.manager.debug;

import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import reactor.core.publisher.Flux;

public class HttpClientDebugSubscriptionProvider implements SubscriptionProvider {
    @Override
    public String id() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String[] getTopicPattern() {
        return new String[0];
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        return null;
    }
}

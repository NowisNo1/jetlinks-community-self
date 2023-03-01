package org.jetlinks.community.network.http.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.community.network.http.client.HttpClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class HttpClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager clientManager;

    static {
        HttpMessageCodec.register();
    }

    @Override
    public String getExecutor() {
        return "http-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new HttpTaskExecutor(context));
    }

    class HttpTaskExecutor extends AbstractTaskExecutor {

        private HttpClientTaskConfiguration config;

        public HttpTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "Http Client";
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new HttpClientTaskConfiguration());
            config.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new HttpClientTaskConfiguration())
                .validate();
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();

            if (config.getType() == PubSubType.producer) {
                disposable.add(context
                    .getInput()
                    .accept()
                    .flatMap(data ->
                        clientManager.<HttpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, config.getClientId())
                            .flatMapMany(client -> RuleDataCodecs
                                .getCodec(HttpMessage.class)
                                .map(codec -> codec.decode(data, config.getPayloadType())
                                    .cast(HttpMessage.class)
                                    .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().warn("can not decode rule data to tcp message:{}", data))))
                                .orElseGet(() -> Flux.just(new HttpMessage(config.getPayloadType().write(data.getData()))))
                                .flatMap(client::send)
                                .onErrorContinue((err, r) -> {
                                    context.onError(err, data).subscribe();
                                })
                                .then()
                            )).subscribe()
                )
                ;
            }
            if (config.getType() == PubSubType.consumer) {
                disposable.add(clientManager.<HttpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, config.getClientId())
                    .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().error("http client {} not found", config.getClientId())))
                    .flatMapMany(HttpClient::subscribe)
                    .doOnNext(msg -> context.getLogger().info("received http client message:{}", config.getPayloadType().read(msg.getPayload())))
                    .map(r -> RuleDataCodecs.getCodec(HttpMessage.class)
                        .map(codec -> codec.encode(r, config.getPayloadType()))
                        .orElse(r.getPayload()))
                    .flatMap(out -> context.getOutput().write(Mono.just(RuleData.create(out))))
                    .onErrorContinue((err, obj) -> context.getLogger().error("consume http message error", err))
                    .subscribe());
            }
            return disposable;
        }
    }
}

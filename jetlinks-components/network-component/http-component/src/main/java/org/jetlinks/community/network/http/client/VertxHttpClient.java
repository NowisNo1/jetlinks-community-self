package org.jetlinks.community.network.http.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.community.network.http.parser.PayloadParser;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class VertxHttpClient implements HttpClient {

    @Getter
    private final String id;
    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();
    private final EmitterProcessor<HttpMessage> processor = EmitterProcessor.create(false);
    private final FluxSink<HttpMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    private final boolean serverClient;
    public volatile NetClient client;
    public NetSocket socket;
    volatile PayloadParser payloadParser;
    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    public VertxHttpClient(String id, boolean serverClient) {
        this.id = id;
        this.serverClient = serverClient;
    }

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public void reset() {
        if (null != payloadParser) {
            payloadParser.reset();
        }
    }

    @Override
    public InetSocketAddress address() {
        return getRemoteAddress();
    }

    @Override
    public Mono<Void> sendMessage(EncodedMessage message) {
        return Mono
            .create((sink) -> {
                if (socket == null) {
                    sink.error(new SocketException("socket closed"));
                    return;
                }
                Buffer buffer = Buffer.buffer(message.getPayload());
                socket.write(buffer, r -> {
                    keepAlive();
                    if (r.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(r.cause());
                    }
                });
            });
    }

    @Override
    public Flux<EncodedMessage> receiveMessage() {
        return this
            .subscribe()
            .cast(EncodedMessage.class);
    }

    @Override
    public void disconnect() {
        shutdown();
    }

    @Override
    public boolean isAlive() {
        return socket != null && (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    /**
     * 接收HTTP消息
     *
     * @param message HTTP消息
     */
    protected void received(HttpMessage message) {
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("http [{}] message pending {} ,drop message:{}", processor.getPending(), getRemoteAddress(), message.toString());
            return;
        }
        sink.next(message);
    }

    @Override
    public Flux<HttpMessage> subscribe() {
        return processor
            .map(Function.identity());
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close http client error", e);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == socket) {
            return null;
        }
        SocketAddress socketAddress = socket.remoteAddress();
        return new InetSocketAddress(socketAddress.host(), socketAddress.port());
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Override
    public void shutdown() {
        log.debug("http client [{}] disconnect", getId());
        synchronized (this) {
            if (null != client) {
                execute(client::close);
                client = null;
            }
            if (null != socket) {
                execute(socket::close);
                this.socket = null;
            }
            if (null != payloadParser) {
                execute(payloadParser::close);
                payloadParser = null;
            }
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
        if (serverClient) {
            processor.onComplete();
        }
    }

    public void setClient(NetClient client) {
        if (this.client != null && this.client != client) {
            this.client.close();
        }
        keepAlive();
        this.client = client;
    }

    /**
     * 设置客户端消息解析器
     *
     * @param payloadParser 消息解析器
     */
    public void setRecordParser(PayloadParser payloadParser) {
        synchronized (this) {
            if (null != this.payloadParser && this.payloadParser != payloadParser) {
                this.payloadParser.close();
            }
            this.payloadParser = payloadParser;
            this.payloadParser
                .handlePayload()
                .onErrorContinue((err, res) -> {
                    log.error(err.getMessage(), err);
                })
                .subscribe(buffer -> received(new HttpMessage(buffer.getByteBuf())));
        }
    }

    /**
     * socket处理
     *
     * @param socket socket
     */
    public void setSocket(NetSocket socket) {
        synchronized (this) {
            Objects.requireNonNull(payloadParser);
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }
            this.socket = socket
                .closeHandler(v -> shutdown())
                .handler(buffer -> {
                    if (log.isDebugEnabled()) {
                        log.debug("handle http client[{}] payload:[{}]",
                            socket.remoteAddress(),
                            Hex.encodeHexString(buffer.getBytes()));
                    }
                    keepAlive();
                    payloadParser.handle(buffer);
                    if (this.socket != socket) {
                        log.warn("http client [{}] memory leak ", socket.remoteAddress());
                        socket.close();
                    }
                });
        }
    }

    @Override
    public Mono<Boolean> send(HttpMessage message) {
        return sendMessage(message)
            .thenReturn(true);
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }
}

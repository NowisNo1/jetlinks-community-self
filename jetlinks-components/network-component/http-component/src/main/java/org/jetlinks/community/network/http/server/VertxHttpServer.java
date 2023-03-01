package org.jetlinks.community.network.http.server;

import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.client.HttpClient;
import org.jetlinks.community.network.http.client.VertxHttpClient;
import org.jetlinks.community.network.http.parser.PayloadParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author LuoXingyue
 * @since 1.0
 **/
@Slf4j
public class VertxHttpServer implements HttpServer {

    @Getter
    private final String id;
    private final EmitterProcessor<HttpClient> processor = EmitterProcessor.create(false);
    private final FluxSink<HttpClient> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    Collection<NetServer> httpServers;
    private Supplier<PayloadParser> parserSupplier;
    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();

    public VertxHttpServer(String id) {
        this.id = id;
    }

    @Override
    public Flux<HttpClient> handleConnection() {
        return processor
            .map(Function.identity());
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close http server error", e);
        }
    }

    public void setParserSupplier(Supplier<PayloadParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    /**
     * 为每个NetServer添加connectHandler
     *
     * @param servers 创建的所有NetServer
     */
    public void setServer(Collection<NetServer> servers) {
        if (this.httpServers != null && !this.httpServers.isEmpty()) {
            shutdown();
        }
        this.httpServers = servers;

        for (NetServer tcpServer : this.httpServers) {
            tcpServer.connectHandler(this::acceptTcpConnection);
        }

    }

    /**
     * HTTP连接处理逻辑
     *
     * @param socket socket
     */
    protected void acceptTcpConnection(NetSocket socket) {
        if (!processor.hasDownstreams()) {
            log.warn("not handler for http client[{}]", socket.remoteAddress());
            socket.close();
            return;
        }
        // 客户端连接处理
        VertxHttpClient client = new VertxHttpClient(id + "_" + socket.remoteAddress(), true);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);
        try {
            // TCP异常和关闭处理
            socket.exceptionHandler(err -> {
                log.error("http server client [{}] error", socket.remoteAddress(), err);
            }).closeHandler((nil) -> {
                log.debug("http server client [{}] closed", socket.remoteAddress());
                client.shutdown();
            });
            // 这个地方是在TCP服务初始化的时候设置的 parserSupplier
            // set方法 org.jetlinks.community.network.http.server.VertxHttpServer.setParserSupplier
            // 调用坐标 org.jetlinks.community.network.http.server.HttpServerProvider.initHttpServer
            client.setRecordParser(parserSupplier.get());
            client.setSocket(socket);
            // client放进了发射器
            sink.next(client);
            log.debug("accept http client [{}] connection", socket.remoteAddress());
        } catch (Exception e) {
            log.error("create http server client error", e);
            client.shutdown();
        }
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != httpServers) {
            for (NetServer HttpServer : httpServers) {
                execute(HttpServer::close);
            }
            httpServers = null;
        }
    }

    @Override
    public boolean isAlive() {
        return httpServers != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}

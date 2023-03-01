package org.jetlinks.community.network.http.server;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.http.client.HttpClient;
import reactor.core.publisher.Flux;

/**
 * HTTP服务
 *
 * @author LuoXingyue
 * @version 1.0
 **/
public interface HttpServer extends Network {

    /**
     * 订阅客户端连接
     *
     * @return 客户端流
     * @see HttpClient
     */
    Flux<HttpClient> handleConnection();

    /**
     * 关闭服务端
     */
    @Override
    void shutdown();
}

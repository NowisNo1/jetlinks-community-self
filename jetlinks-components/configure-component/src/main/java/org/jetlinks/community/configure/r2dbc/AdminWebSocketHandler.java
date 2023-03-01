package org.jetlinks.community.configure.r2dbc;

import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 自定义 WebSocket 连接 处理类
 * @author luo'xing'yue
 * @createTime 2023-02-26
 * @reference https://blog.csdn.net/u011943534/article/details/124157457
 */

@Component
@Slf4j
public class AdminWebSocketHandler implements WebSocketHandler {

    // private static final String CONNECT = "connect";
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 客户端必须要传入 id
        HandshakeInfo handshakeInfo = session.getHandshakeInfo();
        // System.out.println("session.getHandshakeInfo() -> " + session.getHandshakeInfo());
        // System.out.println("handshakeInfo.getUri() -> " + handshakeInfo.getUri());
        // System.out.println("handshakeInfo.getUri().getQuery() -> " + handshakeInfo.getUri().getQuery());
        Map<String, String> queryMap = getQueryMap(handshakeInfo.getUri().getQuery());
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            System.out.println("key :" + entry.getKey() + ", value : " + entry.getValue());
        }

        String id = queryMap.get("id");
        String type = queryMap.get("type");
        // TODO 校验规则
        if(!"".equals(id) && !"".equals(type)){
            Mono<Void> input = session.receive().doOnNext(message -> this.messageHandle(session, message))
                .doOnError(throwable -> log.error("webSocket 异常: " + throwable))
                .doOnComplete(() -> log.info("webSocket结束")).then();
            Mono<Void> output = session.send(
                Flux.create(
                    sink -> {
                        if("server".equals(type)){

                            WebSocketWrap.SERVER = new WebSocketWrap(id, session, sink);
                            WebSocketWrap.SERVER.sendText("你好");

                        }else if("client".equals(type)) {

                            WebSocketWrap.SENDER.put(id, new WebSocketWrap(id, session, sink));
                            WebSocketWrap.SENDER.get(id).sendText("你好");

                        }
                        /*
                             握手后立刻向 node-red 发送信息
                         */
                        // System.out.println("WebSocketWrap.SENDER.size() -> " + WebSocketWrap.SENDER.size());

                    }
                )
            );
            return Mono.zip(input, output).then();
        }else {
            return session.close(new CloseStatus(1016, "连接未通过校验，即将关闭连接"));
        }
    }

    /**
     * @param session 需要保留
     * @param message
     * 规定传输类型为 TEXT 类型，详见 WebSocketWrap 中的 sendText
     */
    @SuppressWarnings(value = "unused")
    private void messageHandle(WebSocketSession session, WebSocketMessage message){
        System.out.println("message -> " + message.getPayloadAsText());
        switch (message.getType()){
            case TEXT:
                /*
                    在此处进行一些逻辑处理：
                        接收 node-red 的信息：
                        1. 确保来自前端的消息正确传输到了 node-red
                        2. 确认消息的处理结果
                        3. ...
                 */
                break;
            case BINARY:
            case PONG:
            case PING:
                break;
            default:
        }
    }

    /**
     * @param queryStr node-red 请求的 uri，目前为 'ws://localhost:8848/local/ws?id={ node-red 端自定义生成的 uuid }'
     * @return 返回拆分的结果
     */
    private Map<String, String> getQueryMap(String queryStr){
        Map<String, String> queryMap = new HashMap<>(4);
        System.out.println("queryStr -> " + queryStr);
        if(!StringUtils.isEmpty(queryStr)){
            String[] queryParam = queryStr.split("&");
            Arrays.stream(queryParam).forEach(s ->{
                String[] kv = s.split("=", 2);
                String value = kv.length == 2 ? kv[1] : "";
                queryMap.put(kv[0], value);
            });
        }
        return queryMap;
    }
}
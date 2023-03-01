package org.jetlinks.community.rule.engine.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.FluxSink;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 WebSocket 信息封装类，用于发送消息和维护连接
 * @author luo'xing'yue
 * @createTime 2023-02-26
 * @reference https://blog.csdn.net/u011943534/article/details/124157457
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class WebSocketWrap {

    public static final Map<String, WebSocketWrap> SENDER = new ConcurrentHashMap<>(); // 客户端 -- 前端
    public static WebSocketWrap SERVER = null; // 服务端 -- node-red 端

    private String id;
    private WebSocketSession session;
    private FluxSink<WebSocketMessage> sink;

    /**
     * 向所有 node-red flow 进行广播（不一定需要用）
     * @param str 广播的内容
     */
    public static void broadcastText(String str){
        SENDER.values().forEach(wrap -> wrap.sendText(str));
    }

    public void sendText(String str){
        System.out.println("str -> " + str);
        sink.next(session.textMessage(str));
    }
    /*
        这是一个定期清除无效连接的函数，不过需要 jdk 11，而且不是必须的，所以删了
    */
//    static{
//        purge();
//    }
//
//    @SuppressWarnings("AlibabaThreadPollCreation")
//    public static void purge(){
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            new ArrayList<>(SENDER.values()).forEach(wrap -> {
//
//            });
//        })
//    }
}
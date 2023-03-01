package org.jetlinks.community.rule.engine.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * 注册类
 * @author luo'xing'yue
 * @createTime 2023-02-26
 * @reference https://blog.csdn.net/u011943534/article/details/124157457
 * 所有的新增 WebSocket 配置都放在 jetlinks-manager/ rule-engine-manager 中
 */


/*
    // 用于生成暂时的 id
        function guid() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
                var r = Math.random() * 16 | 0,
                v = c == 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        }
        const uuid = guid();
        const my_ws = new WebSocket('ws://localhost:8848/local/ws?id=' + uuid);

        // 连接成功
        my_ws.onopen = function(){
            console.log('open', uuid);
            my_ws.send("success");
        }

        // 连接出错
        my_ws.onerror = function(event){
            console.log("error")
        }

        // 收到消息
        my_ws.onmessage = function(event){
            console.log('message', event);
            // setTimeout(() =>{
            //     RED.actions.invoke("core:add-flow")
            // }, 5000)
        }

        // 连接关闭
        my_ws.onclose = function(event){
            console.log('close');
        }
 */

@Configuration
@SuppressWarnings(value = "unused")
public class WebSocketConfiguration {
    /**
     * 将刚才写的处理器注册到 springboot
     * ws的路径设置为 "/local/ws..."
     * @param handler
     * @return
     */
    @Bean
    public HandlerMapping webSocketMapping(final AdminWebSocketHandler handler) {

        final Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/local/ws", handler);
        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}

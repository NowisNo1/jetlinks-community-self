package org.jetlinks.community.rule.engine.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.service.RuleInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Arrays;
import java.util.HashMap;
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

    @Autowired
    private RuleInstanceService instanceService;

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
                .doOnError(throwable -> {
                    log.error("webSocket 异常: " + throwable);
                    WebSocketWrap.SERVER = null;
                })
                .doOnComplete(() -> {
                    log.info("webSocket结束");
                    WebSocketWrap.SERVER = null;
                }).then();
            Mono<Void> output = session.send(
                Flux.create(
                    sink -> {
                        JSONObject jsonObject = new JSONObject();
                        if("server".equals(type)){
                            if(WebSocketWrap.SERVER == null) {
                                WebSocketWrap.SERVER = new WebSocketWrap(id, session, sink);
                            }
                            jsonObject.put("type", "string");
                            jsonObject.put("content", "你好");
                            WebSocketWrap.SERVER.sendText(jsonObject.toJSONString());

                        }else if("client".equals(type)) {

                            jsonObject.put("type", "string");
                            jsonObject.put("content", "你好");
                            WebSocketWrap.SENDER.put(id, new WebSocketWrap(id, session, sink));
                            WebSocketWrap.SENDER.get(id).sendText(jsonObject.toJSONString());

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
                JSONObject jsonObject = JSON.parseObject(message.getPayloadAsText());
                if("string".equals(jsonObject.get("type"))){
                    System.out.println("content in json -> " + jsonObject.get("content"));
                }else{
                    System.out.println("json -> " + jsonObject.toJSONString());
                    String option = jsonObject.getString("option");
                    System.out.println("option -> " + option);
                    switch (option){
                        case "add":
                            // node-red 端执行了添加 flow 的操作 这里需要获取返回的 flowId 用来更新 数据库 model_id

                            instanceService.findById(jsonObject.getString("instanceId"))
                            .doOnNext(entity -> {
                                entity.setModelId(jsonObject.getString("flowId"));
                            })
                            .as(payload -> instanceService.updateById(jsonObject.getString("instanceId"), payload))
                            .then().subscribe();

                            break;
                        default:

                    }
                }
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
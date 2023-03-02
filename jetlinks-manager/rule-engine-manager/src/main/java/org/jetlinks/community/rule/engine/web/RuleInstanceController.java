package org.jetlinks.community.rule.engine.web;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.rule.engine.configuration.WebSocketWrap;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.service.RuleInstanceService;

import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("rule-engine/instance")
@Resource(id = "rule-instance", name = "规则引擎-实例")
@Tag(name = "规则实例")
public class RuleInstanceController implements ReactiveServiceCrudController<RuleInstanceEntity, String> {

    @Autowired
    private RuleInstanceService instanceService;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleEngineModelParser modelParser;

    //获取全部支持的执行器
    @GetMapping("/{instanceId}/tasks")
    @QueryAction
    @Operation(summary = "获取执行中规则的任务信息")
    public Flux<TaskSnapshot> getTasks(@PathVariable String instanceId) {
        return ruleEngine
            .getTasks(instanceId)
            .flatMap(Task::dump);
    }

    /**
     * 查看规则实例的节点
     *
     * @param instanceId 实例ID
     * @return 节点信息
     */
    @GetMapping("/{instanceId}/nodes")
    @QueryAction
    @Operation(summary = "获取规则实例的节点信息")
    public Flux<RuleNodeInfo> getRuleNodeList(@PathVariable String instanceId) {
        Flux<RuleNodeInfo> ans = instanceService
            .findById(instanceId)
            .flatMapIterable(instance -> modelParser
                .parse(instance.getModelType(), instance.getModelMeta())
                .getNodes())
            .map(model -> new RuleNodeInfo(model.getId(), StringUtils.hasLength(model.getName()) ? model.getName() : model.getExecutor()))
            .onErrorResume(err -> Mono.empty());

        return ans;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class RuleNodeInfo {
        private String id;

        private String name;
    }

    @PostMapping("/{id}/_start")
    @ResourceAction(id = "start", name = "启动")
    @Operation(summary = "启动规则")
    public Mono<Boolean> start(@PathVariable String id) {
        Mono<Boolean> ans = instanceService.start(id)
            .thenReturn(true);
        return ans;
    }

    @PostMapping("/{id}/_stop")
    @ResourceAction(id = "stop", name = "停止")
    @Operation(summary = "停止规则")
    public Mono<Boolean> stop(@PathVariable String id) {
        return instanceService.stop(id)
            .thenReturn(true);
    }


    @GetMapping("/{id}/logs")
    @QueryAction
    @QueryOperation(summary = "查询规则日志")
    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryLog(@PathVariable String id,
                                                                @Parameter(hidden = true) QueryParamEntity paramEntity) {
        return paramEntity.toQuery()
            .is("instanceId", id)
            .execute(instanceService::queryExecuteLog);
    }

    @GetMapping("/{id}/events")
    @QueryAction
    @QueryOperation(summary = "查询规则事件")
    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryEvents(@PathVariable String id,
                                                                     @Parameter(hidden = true) QueryParamEntity paramEntity) {
        return paramEntity.toQuery()
            .is("instanceId", id)
            .execute(instanceService::queryExecuteEvent);

    }

    @PostMapping("/{id}/{taskId}/_execute")
    @ResourceAction(id = "execute", name = "执行")
    @QueryOperation(summary = "执行规则")
    public Mono<Void> execute(@PathVariable @Parameter(description = "规则ID") String id,
                              @PathVariable @Parameter(description = "任务ID") String taskId,
                              @RequestBody @Parameter(description = "规则数据") Mono<RuleData> payload) {
        return payload.flatMap(data -> ruleEngine
            .getTasks(id)
            .filter(task -> task.getId().equals(taskId))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(task -> task.execute(data)).then());
    }

    /**
     * @author luo'xing'yue
     * @createTime 2023-02-28
     * @param payload
     * @comment 创建新的 node-red 流程 -> 向 SERVER 发送信息
     */
    @RequestMapping("/create")
    public Mono<RuleInstanceEntity> create(@RequestBody Mono<RuleInstanceEntity> payload){
        System.out.println("params.size() -> " + payload.toString());


        // 返回同时对应着对数据库数据的更新
        return Mono
            .zip(payload, Authentication.currentReactive(), this::applyAuthentication)
            .flatMap(entity -> instanceService.insert(Mono.just(entity)).thenReturn(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("设备ID已存在", err))
            .doOnSuccess(entity ->{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("option", "add");
                jsonObject.put("flowId", "");
                jsonObject.put("instanceId", entity.getId());
                jsonObject.put("instanceName", entity.getName());
                jsonObject.put("instanceInfo", entity.getDescription());
                jsonObject.put("type", "json");
                WebSocketWrap.SERVER.sendText(jsonObject.toJSONString());
            });

    }

    @Override
    public ReactiveCrudService<RuleInstanceEntity, String> getService() {
        return instanceService;
    }
}

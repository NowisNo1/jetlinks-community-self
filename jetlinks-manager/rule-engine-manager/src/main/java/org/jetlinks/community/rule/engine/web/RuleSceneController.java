package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.NotFoundException;
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


/**
 * @author luo'xing'yue
 * @createTime 2023-02-28
 * @reference RuleInstanceController
 */
@RestController
@RequestMapping("rule-engine/scene")
@Resource(id = "scene", name = "规则引擎-场景联动")
@Tag(name = "场景联动")
public class RuleSceneController implements ReactiveServiceCrudController<RuleInstanceEntity, String> {

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
    public Flux<RuleInstanceController.RuleNodeInfo> getRuleNodeList(@PathVariable String instanceId) {
        return instanceService
            .findById(instanceId)
            .flatMapIterable(instance -> modelParser
                .parse(instance.getModelType(), instance.getModelMeta())
                .getNodes())
            .map(model -> new RuleInstanceController.RuleNodeInfo(model.getId(), StringUtils.hasLength(model.getName()) ? model.getName() : model.getExecutor()))
            .onErrorResume(err -> Mono.empty());
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
    @Operation(summary = "启动场景")
    public Mono<Boolean> start(@PathVariable String id) {
        return instanceService.start(id)
            .thenReturn(true);
    }

    @PostMapping("/{id}/_stop")
    @ResourceAction(id = "stop", name = "停止")
    @Operation(summary = "停止场景")
    public Mono<Boolean> stop(@PathVariable String id) {
        return instanceService.stop(id)
            .thenReturn(true);
    }

    @PostMapping("/{id}/{taskId}/_execute")
    @ResourceAction(id = "execute", name = "执行")
    @QueryOperation(summary = "执行场景")
    public Mono<Void> execute(@PathVariable @Parameter(description = "规则ID") String id,
                              @PathVariable @Parameter(description = "任务ID") String taskId,
                              @RequestBody @Parameter(description = "规则数据") Mono<RuleData> payload) {
        return payload.flatMap(data -> ruleEngine
            .getTasks(id)
            .filter(task -> task.getId().equals(taskId))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(task -> task.execute(data)).then());
    }

    @Override
    public ReactiveCrudService<RuleInstanceEntity, String> getService() {
        return instanceService;
    }
}

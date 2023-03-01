package org.jetlinks.community.rule.engine.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.event.handler.RuleEngineLoggerIndexProvider;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.defaults.ScheduleJobCompiler;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
@Slf4j
public class RuleInstanceService extends GenericReactiveCrudService<RuleInstanceEntity, String> implements CommandLineRunner {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleEngineModelParser modelParser;

    @Autowired
    private ElasticSearchService elasticSearchService;

    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryExecuteEvent(QueryParam queryParam) {
        return elasticSearchService.queryPager(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG, queryParam, RuleEngineExecuteEventInfo.class);
    }

    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryExecuteLog(QueryParam queryParam) {
        return elasticSearchService.queryPager(RuleEngineLoggerIndexProvider.RULE_LOG, queryParam, RuleEngineExecuteLogInfo.class);
    }

    public Mono<Void> stop(String id) {
        return this.ruleEngine
            .shutdown(id)
            .then(createUpdate()
                      .set(RuleInstanceEntity::getState, RuleInstanceState.stopped)
                      .where(RuleInstanceEntity::getId, id)
                      .execute())
            .then();
    }


    public Mono<Void> start(String id) {
        System.out.println("获取支持类型" + modelParser.getAllSupportFormat());
        return findById(Mono.just(id))
            .flatMap(this::doStart);
    }
    /**
     * @author Luo
     * @comment 启动规则
     * @Mono.defer -> 每次调用这个函数的时候都是一个新的 Mono
     * @param entity
     * @return
     */
    /*
        // DefaultRuleEngine 中的实现
        public Flux<Task> startRule(String instanceId,
                                RuleModel model) {

            return Flux.fromIterable(new ScheduleJobCompiler(instanceId, model).compile())
                .flatMap(scheduler::schedule)
                .collectList()
                .flatMapIterable(Function.identity())
                .flatMap(task -> task.start().thenReturn(task));
        }
     */

    private Mono<Void> doStart(RuleInstanceEntity entity) {
        return Mono.defer(() -> {
            // 解析出这个规则实例实体的规则模型
            RuleModel model = entity.toRule(modelParser);
            return ruleEngine
                .startRule(entity.getId(), model)
                .then(createUpdate()
                          .set(RuleInstanceEntity::getState, RuleInstanceState.started)
                          .where(entity::getId)
                          .execute()).then();
        });
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .flatMap(id -> this.stop(id).thenReturn(id))
                   .as(super::deleteById);
    }

    @Override
    public void run(String... args) {
        createQuery()
            .where()
            .is(RuleInstanceEntity::getState, RuleInstanceState.started)
            .fetch()
            .flatMap(e -> this
                .doStart(e)
                .onErrorResume(err -> {
                    log.warn("启动规则[{}]失败", e.getName(), e);
                    return Mono.empty();
                }))
            .subscribe();
    }
}

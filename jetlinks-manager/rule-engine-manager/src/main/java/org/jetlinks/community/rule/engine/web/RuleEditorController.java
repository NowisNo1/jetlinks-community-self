package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("rule-engine/editor")
@Resource(id = "rule-editor", name = "规则引擎-设计")
@Tag(name = "规则设计")
public class RuleEditorController implements ReactiveServiceCrudController<RuleInstanceEntity, String> {
    /**
     * author Luoxingyue
     * CreateTime : 2022-08-02 17:25
     * @return
     */
    @PostMapping("/flow/_create")
    @ResourceAction(id = "create", name = "创建")
    @QueryOperation(summary = "创建规则")
    public Mono<Void> create(@PathVariable @Parameter(description = "规则ID") String id,
                             @PathVariable @Parameter(description = "规则名称") String RuleName,
                             @PathVariable @Parameter(description = "规则描述") String Description){
        return null;
    }

    @Override
    public ReactiveCrudService<RuleInstanceEntity, String> getService() {
        return null;
    }
}

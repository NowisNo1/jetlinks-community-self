package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.jetlinks.community.rule.engine.enums.SceneType;
import org.jetlinks.community.rule.engine.scene.Scene;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleSceneModelParser implements RuleModelParserStrategy {
    public static String format = "rule-scene";
    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public RuleModel parse(String modelDefineString) {
        Scene scene = JSON.parseObject(modelDefineString, Scene.class);
        scene.validate();

        RuleModel model = new RuleModel();
        model.setId(scene.getId());
        model.setName(scene.getName());

        RuleNodeModel sceneNode = new RuleNodeModel();
        sceneNode.setId("scene");
        sceneNode.setExecutor("rule_scene");
        sceneNode.setConfiguration(Collections.singletonMap("sql", sceneNode.getRuleId()));
        sceneNode.setName("SCENE");

        model.getNodes().add(sceneNode);

        //错误处理
        List<RuleLink> errorHandler = new ArrayList<>();
        if (!CollectionUtils.isEmpty(scene.getWhenErrorThen())) {
            int index = 0;
            for (Action act : scene.getWhenErrorThen()) {
                if (!StringUtils.hasText(act.getExecutor())) {
                    continue;
                }
                index++;
                RuleNodeModel action = new RuleNodeModel();
                action.setId("error:action:" + index);
                action.setName("错误处理:" + index);
                action.setExecutor(act.getExecutor());
                action.setConfiguration(act.getConfiguration());
                RuleLink link = new RuleLink();
                link.setId(action.getId().concat(":").concat(action.getId()));
                link.setName("错误处理:" + index);
                link.setSource(sceneNode);
                link.setType(RuleConstants.Event.error);
                link.setTarget(action);
                errorHandler.add(link);
                model.getNodes().add(action);
            }
        }

        sceneNode.getEvents().addAll(errorHandler);

        //定时触发
        if (scene.getTrigger() == SceneType.timer) {
            RuleNodeModel timerNode = new RuleNodeModel();
            timerNode.setId("timer");
            timerNode.setExecutor("timer");
            timerNode.setName("定时触发");
            timerNode.setConfiguration(Collections.singletonMap("cron", scene.getCron()));
            timerNode.setRuleId(model.getId());
            RuleLink link = new RuleLink();
            link.setId("sql:timer");
            link.setName("定时触发SQL");
            link.setSource(timerNode);
            link.setTarget(sceneNode);
            timerNode.getOutputs().add(link);
            sceneNode.getInputs().add(link);
            model.getNodes().add(timerNode);
        }


        if (!CollectionUtils.isEmpty(scene.getActions())) {
            int index = 0;
            for (Action operation : scene.getActions()) {
                if (!StringUtils.hasText(operation.getExecutor())) {
                    continue;
                }
                index++;
                RuleNodeModel action = new RuleNodeModel();
                action.setId("action:" + index);
                action.setName("执行动作:" + index);
                action.setExecutor(operation.getExecutor());
                action.setConfiguration(operation.getConfiguration());
                RuleLink link = new RuleLink();
                link.setId(action.getId().concat(":").concat(action.getId()));
                link.setName("执行动作:" + index);
                link.setSource(sceneNode);
                link.setTarget(action);
                model.getNodes().add(action);
                action.getInputs().add(link);
                sceneNode.getOutputs().add(link);

                action.getEvents().addAll(errorHandler);
            }
        }

        return model;
    }
}

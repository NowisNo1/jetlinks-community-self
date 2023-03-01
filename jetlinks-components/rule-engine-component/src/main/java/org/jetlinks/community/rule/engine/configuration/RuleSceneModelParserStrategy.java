package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;

/**
 * 场景联动策略
 * @author luo'xing'yue
 * @CreateTime 2022-08-04 16:22
 */
public class RuleSceneModelParserStrategy implements RuleModelParserStrategy {
    public static String format = "rule-scene";
    @Override
    public String getFormat() {
        return format;
    }

    /**
     * 需要具体的逻辑
     * 可以将Scene类转移到该模块下
     * @param modelDefineString
     * @return
     */
    @Override
    public RuleModel parse(String modelDefineString) {
        RuleModel model = new RuleModel();
        return model;
    }
}

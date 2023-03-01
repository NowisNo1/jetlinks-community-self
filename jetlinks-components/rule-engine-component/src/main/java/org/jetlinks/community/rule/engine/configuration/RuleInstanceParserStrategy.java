package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;

public class RuleInstanceParserStrategy implements RuleModelParserStrategy {
    public static String format = "node-red";

    @Override
    public String getFormat() {
        return format;
    }

    /**
     * 需要具体的逻辑
     * 可以将 instance 类转移到该模块下
     * @param modelDefineString
     * @return
     */
    @Override
    public RuleModel parse(String modelDefineString) {
        return new RuleModel();
    }
}

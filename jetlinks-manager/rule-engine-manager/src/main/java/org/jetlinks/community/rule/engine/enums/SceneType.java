package org.jetlinks.community.rule.engine.enums;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.community.rule.engine.scene.Scene;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

/**
 * 场景联动类型
 * @author luo'xing'yue
 * @CreateTime 2022-08-03 16:02
 */
@Getter
@AllArgsConstructor
@JSONType(deserializer = EnumDict.EnumDictJSONDeserializer.class)
public enum SceneType implements EnumDict<String>{
    timer("定时触发") {
        @Override
        public void validate(Scene scene) {
            Assert.notNull(scene.getCron(), "cron表达式不能为空");
            try {
                new CronSequenceGenerator(scene.getCron());
            } catch (Exception e) {
                throw new IllegalArgumentException("cron表达式格式错误", e);
            }
        }
    },
    device("设备触发") {
        @Override
        public void validate(Scene scene) {
        }
    },
    manual("手动触发") {
        @Override
        public void validate(Scene scene) {
        }
    },
    scene("场景触发"){
        @Override
        public void validate(Scene scene) {
        }
    };

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    public abstract void validate(Scene scene);
}

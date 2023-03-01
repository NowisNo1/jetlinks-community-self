package org.jetlinks.community.rule.engine.scene;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.rule.engine.enums.SceneType;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.model.Action;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;

/**
 * 场景模拟
 *
 * @author LuoXingyue
 * @since 7.28
 */
@Getter
@Setter
public class Scene implements Serializable {


    private static final long serialVersionUID = 8489724178937615194L;

    private String id;

    private String name;

    private String cron;

    private SceneType trigger;

    private List<Action> actions;

    private List<Action> whenErrorThen;

    private Scene Scene;

    private List<DeviceInstanceEntity> AllDevice;

    public void validate() {
        Assert.notNull(trigger, "trigger不能为空");

        trigger.validate(this);
    }
}

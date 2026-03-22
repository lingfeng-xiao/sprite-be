package com.lingfeng.sprite.action;

import java.util.Map;

/**
 * 动作插件接口
 */
public interface ActionPlugin {

    /**
     * 获取插件名称
     */
    String getName();

    /**
     * 执行动作
     *
     * @param params 执行参数
     * @return 执行结果
     */
    ActionResult execute(Map<String, Object> params);
}

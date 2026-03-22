package com.lingfeng.sprite.action.Actions;

import java.util.Map;
import java.util.regex.Pattern;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * 计算器动作
 *
 * 使用 JavaScript 引擎执行数学表达式
 */
public class CalculatorAction implements ActionPlugin {

    private static final Pattern SAFE_EXPRESSION = Pattern.compile("^[0-9+\\-*/().,%^ ]+$");

    @Override
    public String getName() {
        return "Calculator";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        Object expressionObj = params.get("expression");
        if (expressionObj == null) {
            expressionObj = params.get("actionParam");
        }
        if (expressionObj == null) {
            return ActionResult.failure("缺少 expression 参数");
        }

        String expression = expressionObj.toString().trim();

        // 安全检查 - 只允许数字和运算符
        if (!SAFE_EXPRESSION.matcher(expression).matches()) {
            return ActionResult.failure("表达式包含非法字符");
        }

        try {
            // 使用 JavaScript 引擎计算
            var engine = new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expression);

            // 格式化结果
            String resultStr;
            if (result instanceof Double) {
                double d = (Double) result;
                resultStr = (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            } else {
                resultStr = result.toString();
            }

            return ActionResult.success(expression + " = " + resultStr);
        } catch (Exception e) {
            return ActionResult.failure("计算错误: " + e.getMessage());
        }
    }
}

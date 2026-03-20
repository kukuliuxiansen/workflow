package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式评估器
 */
public class ConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);

    public static class EvalResult {
        public boolean matched;
        public String reason;
        public String branchId;

        public EvalResult(boolean matched, String reason, String branchId) {
            this.matched = matched;
            this.reason = reason;
            this.branchId = branchId;
        }
    }

    /**
     * 评估条件表达式
     * 支持: ==, !=, contains, startsWith, endsWith
     */
    public static boolean evaluate(String expression, NodeExecutionContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        try {
            expression = expression.trim();

            // 处理 == 比较
            if (expression.contains("==")) {
                return evaluateEquality(expression, context, true);
            }

            // 处理 != 比较
            if (expression.contains("!=")) {
                return evaluateEquality(expression, context, false);
            }

            // 处理 contains
            if (expression.contains("contains")) {
                return evaluateContains(expression, context);
            }

            // 处理 startsWith / endsWith
            if (expression.contains("startsWith")) {
                return evaluateStartsEndsWith(expression, context, true);
            }
            if (expression.contains("endsWith")) {
                return evaluateStartsEndsWith(expression, context, false);
            }

            // 默认：检查变量是否为真值
            Object value = getVariableValue(expression, context);
            return isTruthy(value);

        } catch (Exception e) {
            logger.warn("表达式评估失败: {} - {}", expression, e.getMessage());
            return false;
        }
    }

    private static boolean evaluateEquality(String expression, NodeExecutionContext context, boolean equals) {
        String[] parts = expression.split(equals ? "==" : "!=");
        if (parts.length != 2) return false;

        String left = parts[0].trim();
        String right = parts[1].trim();

        Object leftValue = getVariableValue(left, context);
        Object rightValue = getVariableValue(right, context);

        // 去除引号
        String leftStr = leftValue != null ? leftValue.toString() : "";
        String rightStr = rightValue != null ? rightValue.toString() : "";

        if (rightStr.startsWith("\"") && rightStr.endsWith("\"")) {
            rightStr = rightStr.substring(1, rightStr.length() - 1);
        }
        if (rightStr.startsWith("'") && rightStr.endsWith("'")) {
            rightStr = rightStr.substring(1, rightStr.length() - 1);
        }

        boolean result = leftStr.equals(rightStr);
        return equals ? result : !result;
    }

    private static boolean evaluateContains(String expression, NodeExecutionContext context) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*contains\\s*[\"']?([^\"']+)[\"']?");
        Matcher matcher = pattern.matcher(expression);
        if (!matcher.find()) return false;

        String varName = matcher.group(1);
        String searchValue = matcher.group(2);

        Object value = getVariableValue(varName, context);
        if (value == null) return false;

        return value.toString().contains(searchValue);
    }

    private static boolean evaluateStartsEndsWith(String expression, NodeExecutionContext context, boolean starts) {
        String keyword = starts ? "startsWith" : "endsWith";
        Pattern pattern = Pattern.compile("(\\w+)\\s*" + keyword + "\\s*[\"']?([^\"']+)[\"']?");
        Matcher matcher = pattern.matcher(expression);
        if (!matcher.find()) return false;

        String varName = matcher.group(1);
        String searchValue = matcher.group(2);

        Object value = getVariableValue(varName, context);
        if (value == null) return false;

        String str = value.toString();
        return starts ? str.startsWith(searchValue) : str.endsWith(searchValue);
    }

    private static Object getVariableValue(String varName, NodeExecutionContext context) {
        if (context == null) return null;

        // 尝试从上下文获取 - 需要转换NodeResult到Object
        Map<String, ?> outputs = context.getPreviousOutputs();
        if (outputs != null && outputs.containsKey(varName)) {
            Object value = outputs.get(varName);
            // 如果是NodeResult，提取output
            if (value instanceof com.openclaw.workflow.engine.model.NodeResult) {
                return ((com.openclaw.workflow.engine.model.NodeResult) value).getOutput();
            }
            return value;
        }

        // 检查特殊变量
        switch (varName.toLowerCase()) {
            case "status":
            case "last_status":
                // 从上下文获取最后状态
                return null;
            case "result":
            case "last_result":
                // 从上下文获取最后结果
                return null;
            default:
                return null;
        }
    }

    private static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
}
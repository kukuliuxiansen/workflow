package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件判断节点处理器
 */
public class ConditionNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConditionNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        String expression = parseExpression(node);

        logger.info("执行条件判断: {}", expression);

        try {
            boolean result = evaluateCondition(expression, context);
            if (result) {
                return NodeResult.success("true");
            } else {
                return NodeResult.failed("条件不满足");
            }
        } catch (Exception e) {
            logger.error("条件判断失败: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    private boolean evaluateCondition(String expression, NodeExecutionContext context) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }

        expression = expression.trim();

        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = getVariableValue(left, context);
                return String.valueOf(leftValue).equals(right);
            }
        } else if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = getVariableValue(left, context);
                return !String.valueOf(leftValue).equals(right);
            }
        }

        return true;
    }

    private Object getVariableValue(String varName, NodeExecutionContext context) {
        if (context.getInput() != null && context.getInput().containsKey(varName)) {
            return context.getInput().get(varName);
        }

        if (context.getPreviousOutputs() != null) {
            for (Map.Entry<String, NodeResult> entry : context.getPreviousOutputs().entrySet()) {
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    return entry.getValue().getOutput();
                }
            }
        }

        return varName;
    }

    private String parseExpression(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());
                if (jsonConfig.has("expression")) {
                    return jsonConfig.get("expression").asText();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }
}
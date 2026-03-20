package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent输出解析器
 * 解析Agent返回的文本，提取思考和动作
 */
public class AgentOutputParser {

    private static final Logger logger = LoggerFactory.getLogger(AgentOutputParser.class);

    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
        "\\[THOUGHT\\](.*?)\\[/THOUGHT\\]", Pattern.DOTALL
    );

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "\\[ACTION\\](.*?)\\[/ACTION\\]", Pattern.DOTALL
    );

    private static final Pattern NODE_DECISION_PATTERN = Pattern.compile(
        "\\[NODE_DECISION\\](.*?)\\[/NODE_DECISION\\]", Pattern.DOTALL
    );

    private final ActionBlockParser actionBlockParser = new ActionBlockParser();
    private final NodeDecisionParser nodeDecisionParser = new NodeDecisionParser();
    private final OutputInferrer outputInferrer = new OutputInferrer();

    /**
     * 解析Agent输出
     */
    public AgentAction parse(String agentOutput) {
        if (agentOutput == null || agentOutput.trim().isEmpty()) {
            return createDefaultAction("输出为空");
        }

        AgentAction action = new AgentAction();

        // 1. 解析思考
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(agentOutput);
        if (thoughtMatcher.find()) {
            action.setThought(thoughtMatcher.group(1).trim());
        }

        // 2. 解析动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(agentOutput);
        if (actionMatcher.find()) {
            String actionBlock = actionMatcher.group(1).trim();
            actionBlockParser.parse(action, actionBlock);
        } else {
            // 尝试解析 NODE_DECISION 格式
            Matcher decisionMatcher = NODE_DECISION_PATTERN.matcher(agentOutput);
            if (decisionMatcher.find()) {
                nodeDecisionParser.parse(action, decisionMatcher.group(1).trim());
            } else {
                // 没有找到明确动作，从内容推断
                return outputInferrer.infer(agentOutput);
            }
        }

        return action;
    }

    /**
     * 创建默认动作
     */
    private AgentAction createDefaultAction(String reason) {
        AgentAction action = new AgentAction();
        action.setTool(DecomposeTool.CONTINUE);
        action.setThought(reason);
        action.setParameters(new HashMap<>());
        return action;
    }
}
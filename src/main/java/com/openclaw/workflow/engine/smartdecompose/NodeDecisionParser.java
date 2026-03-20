package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node Decision 解析器
 * 负责解析 [NODE_DECISION] 格式的输出
 */
public class NodeDecisionParser {

    private static final Logger logger = LoggerFactory.getLogger(NodeDecisionParser.class);

    /**
     * 解析 NODE_DECISION 格式
     */
    public void parse(AgentAction action, String decisionBlock) {
        Map<String, Object> parameters = new HashMap<>();

        // 解析 node_ids
        Pattern nodeIdsPattern = Pattern.compile("node_ids:\\s*(.+?)(?=\\n|$)");
        Matcher nodeIdsMatcher = nodeIdsPattern.matcher(decisionBlock);
        if (nodeIdsMatcher.find()) {
            String nodeIds = nodeIdsMatcher.group(1).trim();
            parameters.put("node_ids", nodeIds);
        }

        // 解析 reason
        Pattern reasonPattern = Pattern.compile("reason:\\s*(.+?)(?=\\n|$)");
        Matcher reasonMatcher = reasonPattern.matcher(decisionBlock);
        if (reasonMatcher.find()) {
            parameters.put("reason", reasonMatcher.group(1).trim());
        }

        // 解析 tool_name
        Pattern toolNamePattern = Pattern.compile("tool:\\s*(.+?)(?=\\n|$)");
        Matcher toolNameMatcher = toolNamePattern.matcher(decisionBlock);
        if (toolNameMatcher.find()) {
            String toolName = toolNameMatcher.group(1).trim();
            DecomposeTool tool = DecomposeTool.fromName(toolName);
            if (tool != null) {
                action.setTool(tool);
            }
        }

        // 如果没有指定工具，根据内容推断
        if (action.getTool() == null) {
            inferToolFromNodeIds(action, parameters);
        }

        action.setParameters(parameters);
    }

    /**
     * 根据 node_ids 推断工具类型
     */
    private void inferToolFromNodeIds(AgentAction action, Map<String, Object> parameters) {
        String nodeIds = (String) parameters.get("node_ids");
        if (nodeIds == null) {
            action.setTool(DecomposeTool.CONTINUE);
            return;
        }

        if (nodeIds.contains("continue")) {
            action.setTool(DecomposeTool.CONTINUE);
        } else if (nodeIds.contains("complete") || nodeIds.contains("finish")) {
            action.setTool(DecomposeTool.MARK_COMPLETE);
        } else if (nodeIds.contains("fail")) {
            action.setTool(DecomposeTool.MARK_FAILED);
        } else {
            action.setTool(DecomposeTool.DECOMPOSE);
            // node_ids 作为子任务ID
            List<Map<String, Object>> subtasks = createSubtasksFromNodeIds(nodeIds);
            parameters.put("subtasks", subtasks);
        }
    }

    /**
     * 从 node_ids 创建子任务列表
     */
    private List<Map<String, Object>> createSubtasksFromNodeIds(String nodeIds) {
        List<Map<String, Object>> subtasks = new ArrayList<>();
        for (String nodeId : nodeIds.split("[,\\s]+")) {
            if (!nodeId.isEmpty()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", nodeId);
                task.put("description", nodeId);
                task.put("priority", 5);
                task.put("dependencies", Collections.emptyList());
                subtasks.add(task);
            }
        }
        return subtasks;
    }
}
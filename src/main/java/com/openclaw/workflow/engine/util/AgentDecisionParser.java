package com.openclaw.workflow.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent决策解析器
 *
 * 用于解析Agent输出中的决策块：
 * [NODE_DECISION]
 * node_ids: xxx, yyy
 * reason: xxx
 * [/NODE_DECISION]
 */
public class AgentDecisionParser {

    private static final Logger logger = LoggerFactory.getLogger(AgentDecisionParser.class);

    // 决策块正则表达式
    private static final Pattern DECISION_PATTERN = Pattern.compile(
        "\\[NODE_DECISION\\]\\s*([\\s\\S]*?)\\s*\\[/NODE_DECISION\\]",
        Pattern.MULTILINE
    );

    // node_ids 行正则
    private static final Pattern NODE_IDS_PATTERN = Pattern.compile(
        "node_ids\\s*:\\s*(.+)",
        Pattern.CASE_INSENSITIVE
    );

    // reason 行正则
    private static final Pattern REASON_PATTERN = Pattern.compile(
        "reason\\s*:\\s*(.+?)(?=\\n|$)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 解析Agent输出中的决策
     *
     * @param agentOutput Agent输出内容
     * @return 决策结果，如果没有找到则返回null
     */
    public static AgentDecision parse(String agentOutput) {
        if (agentOutput == null || agentOutput.isEmpty()) {
            return null;
        }

        try {
            Matcher matcher = DECISION_PATTERN.matcher(agentOutput);
            if (!matcher.find()) {
                logger.debug("Agent输出中未找到决策块");
                return null;
            }

            String decisionBlock = matcher.group(1);
            return parseDecisionBlock(decisionBlock);

        } catch (Exception e) {
            logger.error("解析Agent决策失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析决策块内容
     */
    private static AgentDecision parseDecisionBlock(String block) {
        AgentDecision decision = new AgentDecision();

        // 解析 node_ids
        Matcher nodeIdsMatcher = NODE_IDS_PATTERN.matcher(block);
        if (nodeIdsMatcher.find()) {
            String nodeIdsStr = nodeIdsMatcher.group(1).trim();
            // 支持逗号、空格、分号分隔
            String[] ids = nodeIdsStr.split("[,\\s;]+");
            List<String> nodeIds = new ArrayList<>();
            for (String id : ids) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    nodeIds.add(trimmed);
                }
            }
            decision.setNodeIds(nodeIds);
        }

        // 解析 reason
        Matcher reasonMatcher = REASON_PATTERN.matcher(block);
        if (reasonMatcher.find()) {
            decision.setReason(reasonMatcher.group(1).trim());
        }

        return decision;
    }

    /**
     * 验证决策中的节点ID是否在有效列表中
     *
     * @param decision 决策结果
     * @param validNodeIds 有效的节点ID列表
     * @return 是否有效
     */
    public static boolean validate(AgentDecision decision, List<String> validNodeIds) {
        if (decision == null || decision.getNodeIds() == null || decision.getNodeIds().isEmpty()) {
            return false;
        }

        if (validNodeIds == null || validNodeIds.isEmpty()) {
            return false;
        }

        for (String nodeId : decision.getNodeIds()) {
            if (!validNodeIds.contains(nodeId)) {
                logger.warn("决策中的节点ID无效: {}, 有效节点: {}", nodeId, validNodeIds);
                return false;
            }
        }

        return true;
    }

    /**
     * 提取Agent输出中的决策，如果无效则使用默认节点
     *
     * @param agentOutput Agent输出
     * @param validNodeIds 有效的节点ID列表
     * @param defaultNodeId 默认节点ID
     * @return 决策结果
     */
    public static AgentDecision parseWithDefault(String agentOutput, List<String> validNodeIds, String defaultNodeId) {
        AgentDecision decision = parse(agentOutput);

        if (decision == null || !validate(decision, validNodeIds)) {
            logger.info("使用默认节点: {}", defaultNodeId);
            decision = new AgentDecision();
            decision.setNodeIds(Arrays.asList(defaultNodeId));
            decision.setReason("使用默认节点（Agent未输出有效决策）");
        }

        return decision;
    }

    /**
     * 构建决策提示（注入到Agent提示词中）
     *
     * @param nodeType 节点类型
     * @param nodeName 节点名称
     * @param downstreamNodes 下游节点信息列表
     * @return 决策提示文本
     */
    public static String buildDecisionPrompt(String nodeType, String nodeName, List<DownstreamNode> downstreamNodes) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("\n\n## 工作流决策指引\n\n");
        prompt.append("你正在执行工作流节点「").append(nodeName).append("」。\n\n");

        switch (nodeType.toLowerCase()) {
            case "condition":
                prompt.append("### 当前可选择的分支：\n\n");
                break;
            case "parallel":
                prompt.append("### 当前可选择的分支（可多选）：\n\n");
                break;
            case "loop":
                prompt.append("### 当前循环状态：\n\n");
                prompt.append("- `continue`: 继续下一次迭代\n");
                prompt.append("- `exit`: 退出循环\n\n");
                prompt.append("### 输出格式：\n");
                appendDecisionFormat(prompt, false);
                return prompt.toString();
            default:
                prompt.append("### 当前可选择的下游节点：\n\n");
        }

        if (downstreamNodes != null && !downstreamNodes.isEmpty()) {
            for (DownstreamNode node : downstreamNodes) {
                prompt.append("- `").append(node.getId()).append("`: ");
                prompt.append(node.getName());
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    prompt.append(" - ").append(node.getDescription());
                }
                prompt.append("\n");
            }
        }

        prompt.append("\n### 输出格式：\n");
        appendDecisionFormat(prompt, "parallel".equalsIgnoreCase(nodeType));

        return prompt.toString();
    }

    /**
     * 添加决策格式说明
     */
    private static void appendDecisionFormat(StringBuilder prompt, boolean allowMultiple) {
        prompt.append("```\n");
        prompt.append("[NODE_DECISION]\n");
        if (allowMultiple) {
            prompt.append("node_ids: <节点ID1>, <节点ID2>, ...\n");
        } else {
            prompt.append("node_ids: <节点ID>\n");
        }
        prompt.append("reason: <简短说明选择原因>\n");
        prompt.append("[/NODE_DECISION]\n");
        prompt.append("```\n\n");
        prompt.append("**注意**：\n");
        prompt.append("1. node_ids 必须从上述可选项中选择\n");
        if (allowMultiple) {
            prompt.append("2. 可以选择多个节点（用逗号分隔）以实现并行执行\n");
        }
        prompt.append("3. 如果任务失败或无法继续，选择合适的失败处理节点\n");
    }

    /**
     * Agent决策结果
     */
    public static class AgentDecision {
        private List<String> nodeIds;
        private String reason;

        public AgentDecision() {
            this.nodeIds = new ArrayList<>();
        }

        public List<String> getNodeIds() { return nodeIds; }
        public void setNodeIds(List<String> nodeIds) { this.nodeIds = nodeIds; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getFirstNodeId() {
            return (nodeIds != null && !nodeIds.isEmpty()) ? nodeIds.get(0) : null;
        }

        public boolean hasMultipleNodes() {
            return nodeIds != null && nodeIds.size() > 1;
        }
    }

    /**
     * 下游节点信息
     */
    public static class DownstreamNode {
        private String id;
        private String name;
        private String description;

        public DownstreamNode() {}

        public DownstreamNode(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
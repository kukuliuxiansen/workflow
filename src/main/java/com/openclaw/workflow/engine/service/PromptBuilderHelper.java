package com.openclaw.workflow.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提示词构建辅助工具类
 *
 * 提供模板填充、值格式化、上游输出格式化等通用功能
 */
@Component
public class PromptBuilderHelper {

    private static final Logger logger = LoggerFactory.getLogger(PromptBuilderHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;

    public PromptBuilderHelper(WorkflowNodeRepository nodeRepository,
                              WorkflowEdgeRepository edgeRepository) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * 填充模板变量
     */
    public String fillTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * 格式化值
     */
    public String formatValue(Object value) {
        if (value == null) {
            return "(空)";
        }
        try {
            if (value instanceof String) {
                return (String) value;
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 格式化上游节点输出
     */
    public String formatUpstreamOutputs(Map<String, NodeResult> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return "暂无上游节点输出";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, NodeResult> entry : outputs.entrySet()) {
            sb.append("#### ").append(entry.getKey()).append("\n");
            if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                sb.append("```\n").append(formatValue(entry.getValue().getOutput())).append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取下游节点信息
     */
    public String getDownstreamNodesInfo(String workflowId, String nodeId) {
        if (workflowId == null || nodeId == null) {
            return "无下游节点信息";
        }

        try {
            List<WorkflowEdge> edges =
                    edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, nodeId);

            if (edges.isEmpty()) {
                return "当前节点没有下游节点";
            }

            StringBuilder info = new StringBuilder();
            info.append("当前节点的下游节点：\n");

            for (WorkflowEdge edge : edges) {
                Optional<WorkflowNode> targetNode = nodeRepository.findByWorkflowIdAndId(workflowId, edge.getTargetNodeId());
                if (targetNode.isPresent()) {
                    WorkflowNode node = targetNode.get();
                    info.append("- ").append(node.getName())
                        .append(" (").append(node.getId()).append(", 类型: ").append(node.getType()).append(")\n");
                }
            }

            return info.toString();
        } catch (Exception e) {
            logger.warn("获取下游节点信息失败: {}", e.getMessage());
            return "无法获取下游节点信息";
        }
    }

    /**
     * 构建分支表格
     */
    public String buildBranchTable(List<BranchInfo> branches) {
        if (branches == null || branches.isEmpty()) {
            return "无可用分支";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("| 分支ID | 分支名称 | 条件说明 |\n");
        sb.append("|--------|---------|----------|\n");

        for (BranchInfo branch : branches) {
            sb.append("| `").append(branch.id).append("` | ");
            sb.append(branch.name).append(" | ");
            sb.append(branch.conditionDesc != null ? branch.conditionDesc : branch.description != null ? branch.description : "-");
            sb.append(" |\n");
        }

        return sb.toString();
    }

    /**
     * 构建进度条
     */
    public String buildProgressBar(int currentIteration, int maxIterations) {
        int progress = (int) ((currentIteration * 100.0) / maxIterations);
        StringBuilder progressBar = new StringBuilder("进度: [");
        int filled = progress / 5;
        for (int i = 0; i < 20; i++) {
            progressBar.append(i < filled ? "█" : "░");
        }
        progressBar.append("] ").append(progress).append("%");
        return progressBar.toString();
    }
}
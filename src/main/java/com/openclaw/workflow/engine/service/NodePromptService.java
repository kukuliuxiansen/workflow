package com.openclaw.workflow.engine.service;

import com.openclaw.workflow.engine.config.NodePromptConfig;
import com.openclaw.workflow.engine.model.NodeResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点提示词构建服务
 *
 * 负责根据模板和上下文构建完整的提示词
 * 自动注入上下游节点关系等上下文信息
 */
@Service
public class NodePromptService {

    private final NodePromptConfig promptConfig;
    private final PromptBuilderHelper helper;

    public NodePromptService(NodePromptConfig promptConfig,
                            PromptBuilderHelper helper) {
        this.promptConfig = promptConfig;
        this.helper = helper;
    }

    /**
     * 构建条件判断节点提示词
     */
    public String buildConditionPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            List<BranchInfo> branches,
            String defaultBranch,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("condition");

        if (template == null || template.isEmpty()) {
            template = "条件判断节点 - 请配置决策分支";
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");

        String taskContext = taskDescription != null && !taskDescription.isEmpty()
                ? "\n- 任务描述: " + taskDescription
                : "";
        variables.put("taskContext", taskContext);
        variables.put("upstreamOutputs", helper.formatUpstreamOutputs(previousOutputs));
        variables.put("downstreamNodes", helper.getDownstreamNodesInfo(workflowId, nodeId));
        variables.put("branchTable", helper.buildBranchTable(branches));

        String defaultBranchHint = defaultBranch != null && !defaultBranch.isEmpty()
                ? "**默认分支**: `" + defaultBranch + "` （当无法判断时使用）"
                : "";
        variables.put("defaultBranchHint", defaultBranchHint);

        return helper.fillTemplate(template, variables);
    }

    /**
     * 构建并行执行节点提示词
     */
    public String buildParallelPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String executionMode,
            List<BranchInfo> branches,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("parallel");

        if (template == null || template.isEmpty()) {
            template = "并行执行节点 - 请配置执行分支";
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("executionMode", executionMode != null ? executionMode : "ALL");

        String taskContext = taskDescription != null && !taskDescription.isEmpty()
                ? "\n- 任务描述: " + taskDescription
                : "";
        variables.put("taskContext", taskContext);
        variables.put("upstreamOutputs", helper.formatUpstreamOutputs(previousOutputs));
        variables.put("downstreamNodes", helper.getDownstreamNodesInfo(workflowId, nodeId));
        variables.put("branchTable", helper.buildBranchTable(branches));

        String executionModeDesc = "ALL".equalsIgnoreCase(executionMode)
                ? "- **ALL模式**: 必须执行所有分支"
                : "- **DYNAMIC模式**: 由Agent根据上下文动态选择需要执行的分支";
        variables.put("executionModeDesc", executionModeDesc);

        String decisionRequirements = "ALL".equalsIgnoreCase(executionMode)
                ? "当前是ALL模式，必须选择所有分支。"
                : "1. 根据上游输出分析需要执行哪些分支\n2. 可以选择多个分支，用逗号分隔\n3. 如果不需要执行任何分支，输出 `none`";
        variables.put("decisionRequirements", decisionRequirements);

        return helper.fillTemplate(template, variables);
    }

    /**
     * 构建循环节点提示词
     */
    public String buildLoopPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String loopMode,
            int currentIteration,
            int maxIterations,
            String exitCondition,
            String loopVariable,
            Object loopValue,
            Map<String, NodeResult> previousOutputs,
            String customPrompt) {

        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("loop");

        if (template == null || template.isEmpty()) {
            template = "循环节点 - 请配置循环条件";
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("loopMode", loopMode != null ? loopMode : "condition");
        variables.put("currentIteration", String.valueOf(currentIteration));
        variables.put("maxIterations", String.valueOf(maxIterations));
        variables.put("loopVariable", loopVariable != null ? loopVariable : "item");
        variables.put("currentValue", loopValue != null ? helper.formatValue(loopValue) : "(无)");
        variables.put("progressBar", helper.buildProgressBar(currentIteration, maxIterations));

        String exitCond = exitCondition != null && !exitCondition.isEmpty()
                ? exitCondition
                : "（未配置具体退出条件，由Agent根据执行结果判断）";
        variables.put("exitCondition", exitCond);
        variables.put("loopResults", helper.formatUpstreamOutputs(previousOutputs));

        return helper.fillTemplate(template, variables);
    }

    /**
     * 构建Agent执行节点提示词
     */
    public String buildAgentPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String taskDescription,
            String globalPrompt,
            String nodePrompt,
            Map<String, NodeResult> previousOutputs) {

        String template = promptConfig.getPromptTemplate("agent_execution");

        if (template == null || template.isEmpty()) {
            template = "Agent执行节点 - 请配置任务提示";
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("taskDescription", taskDescription != null ? taskDescription : "无具体任务描述");
        variables.put("globalPrompt", globalPrompt != null && !globalPrompt.isEmpty() ? globalPrompt : "无全局提示");
        variables.put("nodePrompt", nodePrompt != null && !nodePrompt.isEmpty() ? nodePrompt : "无节点特定提示");
        variables.put("upstreamOutputs", helper.formatUpstreamOutputs(previousOutputs));
        variables.put("downstreamNodes", helper.getDownstreamNodesInfo(workflowId, nodeId));

        return helper.fillTemplate(template, variables);
    }
}
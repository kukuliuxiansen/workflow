package com.openclaw.workflow.engine.util;

import com.openclaw.workflow.engine.model.NodeResult;

import java.util.List;
import java.util.Map;

/**
 * 节点提示词构建器（门面类）
 *
 * 委托调用各专门类型的提示词构建器
 * @see ConditionPromptBuilder 条件判断节点
 * @see ParallelPromptBuilder 并行执行节点
 * @see LoopPromptBuilder 循环节点
 */
public class NodePromptBuilder {

    /**
     * 分支信息别名，便于调用方使用
     */
    public static class BranchInfo extends PromptUtils.BranchInfo {
        public BranchInfo(String id, String name, String description) {
            super(id, name, description);
        }

        public BranchInfo(String id, String name, String description, String conditionDesc) {
            super(id, name, description, conditionDesc);
        }
    }

    /**
     * 构建条件判断节点提示词
     */
    public static String buildConditionPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            List<BranchInfo> branches,
            String defaultBranch,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {
        return ConditionPromptBuilder.build(
                workflowId, executionId, nodeId, nodeName,
                convertBranches(branches), defaultBranch,
                previousOutputs, taskDescription, customPrompt);
    }

    /**
     * 构建并行执行节点提示词
     */
    public static String buildParallelPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String executionMode,
            List<BranchInfo> branches,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {
        return ParallelPromptBuilder.build(
                workflowId, executionId, nodeId, nodeName, executionMode,
                convertBranches(branches), previousOutputs, taskDescription, customPrompt);
    }

    /**
     * 构建循环节点提示词
     */
    public static String buildLoopPrompt(
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
        return LoopPromptBuilder.build(
                workflowId, executionId, nodeId, nodeName, loopMode,
                currentIteration, maxIterations, exitCondition,
                loopVariable, loopValue, previousOutputs, customPrompt);
    }

    /**
     * 转换分支信息列表类型
     */
    @SuppressWarnings("unchecked")
    private static List<PromptUtils.BranchInfo> convertBranches(List<BranchInfo> branches) {
        return (List<PromptUtils.BranchInfo>) (List<?>) branches;
    }
}
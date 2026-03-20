package com.openclaw.workflow.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点默认提示词配置
 *
 * 为每种节点类型提供可定制的默认提示词模板
 * 用户可以通过配置文件或API修改这些模板
 */
@Component
public class NodePromptConfig {

    private static final Logger logger = LoggerFactory.getLogger(NodePromptConfig.class);

    // 存储每种节点类型的默认提示词模板
    private Map<String, String> defaultPrompts = new HashMap<>();

    // 存储用户自定义的提示词模板
    private Map<String, String> customPrompts = new HashMap<>();

    public NodePromptConfig() {
        initDefaultPrompts();
    }

    /**
     * 初始化默认提示词模板
     */
    private void initDefaultPrompts() {
        // 条件判断节点默认提示词
        defaultPrompts.put("condition",
            "## 条件判断决策任务\n" +
            "\n" +
            "你当前在工作流的【{nodeName}】节点，需要根据上游输出决定执行哪个分支。\n" +
            "\n" +
            "### 当前工作流上下文\n" +
            "- 工作流ID: {workflowId}\n" +
            "- 执行ID: {executionId}\n" +
            "- 当前节点: {nodeName} ({nodeId})\n" +
            "{taskContext}\n" +
            "\n" +
            "### 上游节点输出\n" +
            "{upstreamOutputs}\n" +
            "\n" +
            "### 下游节点\n" +
            "{downstreamNodes}\n" +
            "\n" +
            "### 可选分支\n" +
            "请从以下分支中选择**一个**执行：\n" +
            "\n" +
            "{branchTable}\n" +
            "\n" +
            "{defaultBranchHint}\n" +
            "\n" +
            "### 决策要求\n" +
            "1. 仔细分析上游节点的输出内容\n" +
            "2. 根据上述条件说明，判断应该走哪个分支\n" +
            "3. **必须选择一个分支**，不能不选\n" +
            "4. 选择后请说明选择原因\n" +
            "\n" +
            "### 输出格式\n" +
            "请严格按照以下格式输出你的决策：\n" +
            "\n" +
            "```\n" +
            "[NODE_DECISION]\n" +
            "node_ids: {选择的分支ID}\n" +
            "reason: {选择该分支的原因}\n" +
            "[/NODE_DECISION]\n" +
            "```\n");

        // 并行执行节点默认提示词
        defaultPrompts.put("parallel",
            "## 并行执行决策任务\n" +
            "\n" +
            "你当前在工作流的【{nodeName}】节点，需要决定并行执行哪些分支。\n" +
            "\n" +
            "### 当前工作流上下文\n" +
            "- 工作流ID: {workflowId}\n" +
            "- 执行ID: {executionId}\n" +
            "- 当前节点: {nodeName} ({nodeId})\n" +
            "- 执行模式: **{executionMode}**\n" +
            "{taskContext}\n" +
            "\n" +
            "### 上游节点输出\n" +
            "{upstreamOutputs}\n" +
            "\n" +
            "### 下游节点\n" +
            "{downstreamNodes}\n" +
            "\n" +
            "### 执行模式说明\n" +
            "{executionModeDesc}\n" +
            "\n" +
            "### 可选分支\n" +
            "{branchTable}\n" +
            "\n" +
            "### 决策要求\n" +
            "{decisionRequirements}\n" +
            "\n" +
            "### 输出格式\n" +
            "```\n" +
            "[NODE_DECISION]\n" +
            "node_ids: {分支ID1}, {分支ID2}, ...\n" +
            "reason: {选择这些分支的原因}\n" +
            "[/NODE_DECISION]\n" +
            "```\n");

        // 循环节点默认提示词
        defaultPrompts.put("loop",
            "## 循环决策任务\n" +
            "\n" +
            "你当前在工作流的【{nodeName}】节点，需要判断是否继续循环。\n" +
            "\n" +
            "### 循环状态\n" +
            "- 当前迭代: **第 {currentIteration} 次**\n" +
            "- 最大迭代: {maxIterations} 次\n" +
            "- 循环模式: {loopMode}\n" +
            "\n" +
            "{progressBar}\n" +
            "\n" +
            "### 当前循环变量\n" +
            "- 变量名: `{loopVariable}`\n" +
            "- 当前值:\n" +
            "```\n" +
            "{currentValue}\n" +
            "```\n" +
            "\n" +
            "### 本次循环执行结果\n" +
            "{loopResults}\n" +
            "\n" +
            "### 退出条件\n" +
            "{exitCondition}\n" +
            "\n" +
            "### 决策选项\n" +
            "| 选项 | 说明 |\n" +
            "|------|------|\n" +
            "| `continue` | 继续执行下一次循环 |\n" +
            "| `exit` | 退出循环，继续后续流程 |\n" +
            "\n" +
            "### 决策要求\n" +
            "1. 分析当前循环的执行结果\n" +
            "2. 判断是否满足退出条件\n" +
            "3. 如果达到最大迭代次数，应该选择退出\n" +
            "4. 如果遇到错误无法继续，应该选择退出\n" +
            "\n" +
            "### 输出格式\n" +
            "```\n" +
            "[NODE_DECISION]\n" +
            "node_ids: continue 或 exit\n" +
            "reason: {继续或退出的原因}\n" +
            "[/NODE_DECISION]\n" +
            "```\n");

        // Agent执行节点默认提示词
        defaultPrompts.put("agent_execution",
            "## Agent执行任务\n" +
            "\n" +
            "你是一个工作流执行节点，需要完成指定的任务。\n" +
            "\n" +
            "### 工作流上下文\n" +
            "- 工作流ID: {workflowId}\n" +
            "- 执行ID: {executionId}\n" +
            "- 当前节点: {nodeName} ({nodeId})\n" +
            "\n" +
            "### 任务信息\n" +
            "{taskDescription}\n" +
            "\n" +
            "### 全局提示\n" +
            "{globalPrompt}\n" +
            "\n" +
            "### 节点配置的提示\n" +
            "{nodePrompt}\n" +
            "\n" +
            "### 上游节点输出\n" +
            "{upstreamOutputs}\n" +
            "\n" +
            "### 下游节点\n" +
            "{downstreamNodes}\n" +
            "\n" +
            "请执行上述任务，并输出执行结果。\n");

        // 人工审核节点默认提示词
        defaultPrompts.put("human_review",
            "## 人工审核任务\n" +
            "\n" +
            "当前工作流执行到【{nodeName}】节点，需要人工审核。\n" +
            "\n" +
            "### 工作流上下文\n" +
            "- 工作流ID: {workflowId}\n" +
            "- 执行ID: {executionId}\n" +
            "- 当前节点: {nodeName} ({nodeId})\n" +
            "\n" +
            "### 审核信息\n" +
            "{reviewMessage}\n" +
            "\n" +
            "### 上游节点输出\n" +
            "{upstreamOutputs}\n" +
            "\n" +
            "### 审核选项\n" +
            "- 通过: 继续执行工作流\n" +
            "- 拒绝: 终止或走拒绝分支\n" +
            "\n" +
            "等待人工审核...\n");

        logger.info("已加载 {} 种节点类型的默认提示词模板", defaultPrompts.size());
    }

    /**
     * 获取节点提示词模板
     * 优先使用用户自定义的模板，其次使用默认模板
     */
    public String getPromptTemplate(String nodeType) {
        // 先检查用户自定义模板
        if (customPrompts.containsKey(nodeType)) {
            return customPrompts.get(nodeType);
        }
        // 返回默认模板
        return defaultPrompts.getOrDefault(nodeType, "");
    }

    /**
     * 设置自定义提示词模板
     */
    public void setCustomPrompt(String nodeType, String template) {
        customPrompts.put(nodeType, template);
        logger.info("已更新节点类型 {} 的自定义提示词模板", nodeType);
    }

    /**
     * 重置为默认提示词模板
     */
    public void resetToDefault(String nodeType) {
        customPrompts.remove(nodeType);
        logger.info("已重置节点类型 {} 为默认提示词模板", nodeType);
    }

    /**
     * 获取所有默认提示词模板
     */
    public Map<String, String> getAllDefaultPrompts() {
        return new HashMap<>(defaultPrompts);
    }

    /**
     * 获取所有自定义提示词模板
     */
    public Map<String, String> getAllCustomPrompts() {
        return new HashMap<>(customPrompts);
    }

    /**
     * 批量设置自定义提示词
     */
    public void setCustomPrompts(Map<String, String> prompts) {
        customPrompts.putAll(prompts);
        logger.info("已批量更新 {} 个节点类型的提示词模板", prompts.size());
    }
}
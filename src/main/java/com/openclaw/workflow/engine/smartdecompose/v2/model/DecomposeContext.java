package com.openclaw.workflow.engine.smartdecompose.v2.model;

import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * 分解执行上下文
 *
 * SmartDecompose 执行过程中的核心数据结构，保存所有执行状态和配置。
 * 整个执行生命周期中使用同一个 Context 实例。
 */
public class DecomposeContext {

    // ==================== 基本信息 ====================

    /** 执行ID */
    private String executionId;

    /** 工作流ID */
    private String workflowId;

    /** 节点ID */
    private String nodeId;

    /** 项目路径 */
    private String projectPath;

    /** 技术栈 */
    private String techStack;

    // ==================== 任务队列 ====================

    /** 待执行任务队列 */
    private Deque<SubTask> taskQueue;

    /** 已完成任务列表 */
    private List<SubTask> completedTasks;

    /** 失败任务列表 */
    private List<SubTask> failedTasks;

    /** 当前处理的任务 */
    private SubTask currentTask;

    // ==================== 迭代控制 ====================

    /** 当前迭代次数 */
    private int iterationCount;

    /** 最大迭代次数 */
    private int maxIterations;

    // ==================== 重试配置 ====================

    /** 最大重试次数 */
    private int maxRetries;

    /** 最大任务总数 */
    private int maxTotalTasks = 1000;

    /** 是否需要人工审核 */
    private boolean requireManualReview;

    // ==================== 状态信息 ====================

    /** 当前状态 */
    private DecomposeStatus status;

    /** 错误信息 */
    private String errorMessage;

    // ==================== 提示词模板 ====================

    /** 模板ID，关联 TemplateConfig */
    private String templateId;

    /** 决策模板ID */
    private String decisionTemplateId;

    /** 审核模板ID */
    private String reviewTemplateId;

    /** 重试模板ID */
    private String retryTemplateId;

    /** 决策模板内容（运行时加载） */
    private String decisionTemplateContent;

    /** 审核模板内容（运行时加载） */
    private String reviewTemplateContent;

    /** 重试模板内容（运行时加载） */
    private String retryTemplateContent;

    // ==================== 场景参数 ====================

    /** 场景ID */
    private String sceneId;

    /** 决策阈值（分钟），任务预估超过此值则拆分 */
    private int decisionThresholdMinutes = 8;

    /** execute 输出格式模板 */
    private String outputFormatExecute;

    /** split 输出格式模板 */
    private String outputFormatSplit;

    // ==================== OpenClaw 会话 ====================

    /** OpenClaw 会话ID */
    private String openClawSessionId;

    // ==================== 人工审核 ====================

    /** 当前人工审核ID */
    private String manualReviewId;

    // ==================== 构造方法 ====================

    public DecomposeContext() {
        this.taskQueue = new LinkedList<>();
        this.completedTasks = new ArrayList<>();
        this.failedTasks = new ArrayList<>();
        this.iterationCount = 0;
        this.maxIterations = 50;
        this.maxRetries = 10;
        this.maxTotalTasks = 1000;
        this.requireManualReview = true;
        this.status = DecomposeStatus.RUNNING;
    }

    // ==================== 业务方法 ====================

    /**
     * 增加迭代次数
     */
    public void incrementIteration() {
        // 实现思路：iterationCount++
        this.iterationCount++;
    }

    /**
     * 添加已完成任务
     *
     * @param task 已完成的任务
     */
    public void addCompletedTask(SubTask task) {
        // 实现思路：将任务添加到 completedTasks 列表
        if (task != null) {
            this.completedTasks.add(task);
        }
    }

    /**
     * 添加失败任务
     *
     * @param task 失败的任务
     */
    public void addFailedTask(SubTask task) {
        // 实现思路：将任务添加到 failedTasks 列表
        if (task != null) {
            this.failedTasks.add(task);
        }
    }

    /**
     * 获取已完成任务的信息摘要
     *
     * 用于构建提示词中的已完成任务部分
     *
     * @return 已完成任务信息字符串
     */
    public String getCompletedTasksInfo() {
        // 实现思路：
        // 1. 遍历 completedTasks
        // 2. 格式化为 "1. 任务描述 ✓\n结果: xxx" 格式
        // 3. 空列表返回 "暂无"
        if (completedTasks.isEmpty()) {
            return "暂无";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < completedTasks.size(); i++) {
            SubTask task = completedTasks.get(i);
            sb.append(i + 1).append(". ").append(task.getDescription()).append(" ✓\n");
            if (task.getExecutionResult() != null) {
                sb.append("   结果: ").append(task.getExecutionResult()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取任务总数（队列 + 已完成 + 失败）
     */
    public int getTotalTaskCount() {
        return taskQueue.size() + completedTasks.size() + failedTasks.size();
    }

    // ==================== Getters & Setters ====================

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    public Deque<SubTask> getTaskQueue() { return taskQueue; }
    public void setTaskQueue(Deque<SubTask> taskQueue) { this.taskQueue = taskQueue; }

    public List<SubTask> getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(List<SubTask> completedTasks) { this.completedTasks = completedTasks; }

    public List<SubTask> getFailedTasks() { return failedTasks; }
    public void setFailedTasks(List<SubTask> failedTasks) { this.failedTasks = failedTasks; }

    public SubTask getCurrentTask() { return currentTask; }
    public void setCurrentTask(SubTask currentTask) { this.currentTask = currentTask; }

    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getMaxTotalTasks() { return maxTotalTasks; }
    public void setMaxTotalTasks(int maxTotalTasks) { this.maxTotalTasks = maxTotalTasks; }

    public boolean isRequireManualReview() { return requireManualReview; }
    public void setRequireManualReview(boolean requireManualReview) { this.requireManualReview = requireManualReview; }

    public DecomposeStatus getStatus() { return status; }
    public void setStatus(DecomposeStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getDecisionTemplateId() { return decisionTemplateId; }
    public void setDecisionTemplateId(String decisionTemplateId) { this.decisionTemplateId = decisionTemplateId; }

    public String getReviewTemplateId() { return reviewTemplateId; }
    public void setReviewTemplateId(String reviewTemplateId) { this.reviewTemplateId = reviewTemplateId; }

    public String getRetryTemplateId() { return retryTemplateId; }
    public void setRetryTemplateId(String retryTemplateId) { this.retryTemplateId = retryTemplateId; }

    public String getDecisionTemplateContent() { return decisionTemplateContent; }
    public void setDecisionTemplateContent(String decisionTemplateContent) { this.decisionTemplateContent = decisionTemplateContent; }

    public String getReviewTemplateContent() { return reviewTemplateContent; }
    public void setReviewTemplateContent(String reviewTemplateContent) { this.reviewTemplateContent = reviewTemplateContent; }

    public String getRetryTemplateContent() { return retryTemplateContent; }
    public void setRetryTemplateContent(String retryTemplateContent) { this.retryTemplateContent = retryTemplateContent; }

    public String getOpenClawSessionId() { return openClawSessionId; }
    public void setOpenClawSessionId(String openClawSessionId) { this.openClawSessionId = openClawSessionId; }

    public String getManualReviewId() { return manualReviewId; }
    public void setManualReviewId(String manualReviewId) { this.manualReviewId = manualReviewId; }

    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }

    public int getDecisionThresholdMinutes() { return decisionThresholdMinutes; }
    public void setDecisionThresholdMinutes(int decisionThresholdMinutes) { this.decisionThresholdMinutes = decisionThresholdMinutes; }

    public String getOutputFormatExecute() { return outputFormatExecute; }
    public void setOutputFormatExecute(String outputFormatExecute) { this.outputFormatExecute = outputFormatExecute; }

    public String getOutputFormatSplit() { return outputFormatSplit; }
    public void setOutputFormatSplit(String outputFormatSplit) { this.outputFormatSplit = outputFormatSplit; }
}
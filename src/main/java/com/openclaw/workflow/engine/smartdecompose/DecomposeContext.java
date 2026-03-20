package com.openclaw.workflow.engine.smartdecompose;

import java.util.*;

/**
 * 智能分解执行上下文
 */
public class DecomposeContext {

    // ============ 任务管理 ============
    private Stack<TaskState> taskStack;         // 任务栈（待处理）
    private List<TaskState> completedTasks;     // 已完成任务列表
    private List<TaskState> failedTasks;        // 失败任务列表
    private TaskState currentTask;              // 当前正在执行的任务
    private Map<String, TaskState> taskMap;     // 任务ID -> 任务映射

    // ============ 循环控制 ============
    private int iterationCount;                 // 当前迭代次数
    private int maxIterations;                  // 最大迭代次数（默认100）
    private int maxDepth;                        // 最大递归深度（默认5）

    // ============ 状态追踪 ============
    private List<DecisionRecord> decisionHistory; // 决策历史
    private Map<String, Object> contextCache;   // 上下文缓存（存储关键信息）
    private List<ToolCallRecord> toolCalls;     // 工具调用记录

    // ============ 输出收集 ============
    private StringBuilder outputBuffer;         // 输出缓冲
    private List<Artifact> artifacts;           // 生成的产物（文件等）

    // ============ 执行状态 ============
    private DecomposeStatus status;             // RUNNING/COMPLETED/FAILED/TIMEOUT
    private String errorMessage;                // 错误信息

    // ============ 关联信息 ============
    private String executionId;
    private String workflowId;
    private String nodeId;
    private String projectPath;
    private String globalPrompt;

    public enum DecomposeStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        TIMEOUT,
        WAITING_USER_INPUT
    }

    public DecomposeContext() {
        this.taskStack = new Stack<>();
        this.completedTasks = new ArrayList<>();
        this.failedTasks = new ArrayList<>();
        this.taskMap = new HashMap<>();
        this.decisionHistory = new ArrayList<>();
        this.contextCache = new HashMap<>();
        this.toolCalls = new ArrayList<>();
        this.outputBuffer = new StringBuilder();
        this.artifacts = new ArrayList<>();
        this.status = DecomposeStatus.RUNNING;
        this.maxIterations = 50;
        this.maxDepth = 3;
    }

    public void incrementIteration() {
        this.iterationCount++;
    }

    public boolean isIterationExceeded() {
        return iterationCount >= maxIterations;
    }

    // Getters and Setters
    public Stack<TaskState> getTaskStack() { return taskStack; }
    public void setTaskStack(Stack<TaskState> taskStack) { this.taskStack = taskStack; }
    public List<TaskState> getCompletedTasks() { return completedTasks; }
    public List<TaskState> getFailedTasks() { return failedTasks; }
    public TaskState getCurrentTask() { return currentTask; }
    public void setCurrentTask(TaskState currentTask) { this.currentTask = currentTask; }
    public Map<String, TaskState> getTaskMap() { return taskMap; }

    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public List<DecisionRecord> getDecisionHistory() { return decisionHistory; }
    public Map<String, Object> getContextCache() { return contextCache; }
    public List<ToolCallRecord> getToolCalls() { return toolCalls; }

    public StringBuilder getOutputBuffer() { return outputBuffer; }
    public List<Artifact> getArtifacts() { return artifacts; }

    public DecomposeStatus getStatus() { return status; }
    public void setStatus(DecomposeStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
    public String getGlobalPrompt() { return globalPrompt; }
    public void setGlobalPrompt(String globalPrompt) { this.globalPrompt = globalPrompt; }

    /**
     * 决策记录
     */
    public static class DecisionRecord {
        private int iteration;
        private String taskId;
        private String thought;
        private String action;
        private ActionResult result;
        private long timestamp;

        public int getIteration() { return iteration; }
        public void setIteration(int iteration) { this.iteration = iteration; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getThought() { return thought; }
        public void setThought(String thought) { this.thought = thought; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public ActionResult getResult() { return result; }
        public void setResult(ActionResult result) { this.result = result; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 工具调用记录
     */
    public static class ToolCallRecord {
        private String toolName;
        private Map<String, Object> parameters;
        private Object result;
        private long durationMs;
        private boolean success;

        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    /**
     * 产物记录
     */
    public static class Artifact {
        private String type;
        private String path;
        private String content;
        private Date createTime;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
    }
}
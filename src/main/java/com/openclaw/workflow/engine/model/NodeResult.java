package com.openclaw.workflow.engine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行结果
 */
public class NodeResult {

    private String status; // success, failed, retry
    private Object output;
    private String error;
    private boolean waitingRetry;

    // 新增：支持多节点选择（用于条件、并行、循环节点）
    private List<String> nextNodeIds;

    // 新增：决策信息（Agent决策协议）
    private String decisionReason;

    // 新增：循环上下文
    private LoopContext loopContext;

    // 新增：并行上下文
    private ParallelContext parallelContext;

    // 新增：人工审核上下文
    private ReviewContext reviewContext;

    // 新增：额外的元数据
    private Map<String, Object> metadata;

    public NodeResult() {
        this.nextNodeIds = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public static NodeResult success(Object output) {
        NodeResult result = new NodeResult();
        result.setStatus("success");
        result.setOutput(output);
        return result;
    }

    public static NodeResult success(Object output, String nextNodeId) {
        NodeResult result = new NodeResult();
        result.setStatus("success");
        result.setOutput(output);
        result.addNextNodeId(nextNodeId);
        return result;
    }

    public static NodeResult successWithNodes(Object output, List<String> nextNodeIds) {
        NodeResult result = new NodeResult();
        result.setStatus("success");
        result.setOutput(output);
        result.setNextNodeIds(nextNodeIds);
        return result;
    }

    public static NodeResult failed(String error) {
        NodeResult result = new NodeResult();
        result.setStatus("failed");
        result.setError(error);
        return result;
    }

    public static NodeResult failed(Object output, String error) {
        NodeResult result = new NodeResult();
        result.setStatus("failed");
        result.setOutput(output);
        result.setError(error);
        return result;
    }

    public static NodeResult retry() {
        NodeResult result = new NodeResult();
        result.setStatus("retry");
        return result;
    }

    public static NodeResult waitingReview(ReviewContext reviewContext) {
        NodeResult result = new NodeResult();
        result.setStatus("waiting_review");
        result.setReviewContext(reviewContext);
        return result;
    }

    // 添加下一个节点ID
    public void addNextNodeId(String nodeId) {
        if (this.nextNodeIds == null) {
            this.nextNodeIds = new ArrayList<>();
        }
        if (nodeId != null && !this.nextNodeIds.contains(nodeId)) {
            this.nextNodeIds.add(nodeId);
        }
    }

    // 判断是否有多个下一节点
    public boolean hasMultipleNextNodes() {
        return nextNodeIds != null && nextNodeIds.size() > 1;
    }

    // 判断是否有下一节点
    public boolean hasNextNode() {
        return nextNodeIds != null && !nextNodeIds.isEmpty();
    }

    // 获取第一个下一节点
    public String getFirstNextNodeId() {
        return (nextNodeIds != null && !nextNodeIds.isEmpty()) ? nextNodeIds.get(0) : null;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getOutput() { return output; }
    public void setOutput(Object output) { this.output = output; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public boolean isWaitingRetry() { return waitingRetry; }
    public void setWaitingRetry(boolean waitingRetry) { this.waitingRetry = waitingRetry; }

    public List<String> getNextNodeIds() { return nextNodeIds; }
    public void setNextNodeIds(List<String> nextNodeIds) { this.nextNodeIds = nextNodeIds; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public LoopContext getLoopContext() { return loopContext; }
    public void setLoopContext(LoopContext loopContext) { this.loopContext = loopContext; }

    public ParallelContext getParallelContext() { return parallelContext; }
    public void setParallelContext(ParallelContext parallelContext) { this.parallelContext = parallelContext; }

    public ReviewContext getReviewContext() { return reviewContext; }
    public void setReviewContext(ReviewContext reviewContext) { this.reviewContext = reviewContext; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 循环上下文
     */
    public static class LoopContext {
        private int currentIteration;
        private int maxIterations;
        private Object currentValue;
        private String loopVariable;
        private List<Object> iterationResults;
        private boolean shouldContinue;

        public LoopContext() {
            this.iterationResults = new ArrayList<>();
        }

        // Getters and Setters
        public int getCurrentIteration() { return currentIteration; }
        public void setCurrentIteration(int currentIteration) { this.currentIteration = currentIteration; }

        public int getMaxIterations() { return maxIterations; }
        public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

        public Object getCurrentValue() { return currentValue; }
        public void setCurrentValue(Object currentValue) { this.currentValue = currentValue; }

        public String getLoopVariable() { return loopVariable; }
        public void setLoopVariable(String loopVariable) { this.loopVariable = loopVariable; }

        public List<Object> getIterationResults() { return iterationResults; }
        public void setIterationResults(List<Object> iterationResults) { this.iterationResults = iterationResults; }

        public boolean isShouldContinue() { return shouldContinue; }
        public void setShouldContinue(boolean shouldContinue) { this.shouldContinue = shouldContinue; }
    }

    /**
     * 并行上下文
     */
    public static class ParallelContext {
        private String parallelNodeId;
        private List<BranchInfo> branches;
        private String mergeNodeId;

        public ParallelContext() {
            this.branches = new ArrayList<>();
        }

        // Getters and Setters
        public String getParallelNodeId() { return parallelNodeId; }
        public void setParallelNodeId(String parallelNodeId) { this.parallelNodeId = parallelNodeId; }

        public List<BranchInfo> getBranches() { return branches; }
        public void setBranches(List<BranchInfo> branches) { this.branches = branches; }

        public String getMergeNodeId() { return mergeNodeId; }
        public void setMergeNodeId(String mergeNodeId) { this.mergeNodeId = mergeNodeId; }

        public void addBranch(BranchInfo branch) {
            if (this.branches == null) {
                this.branches = new ArrayList<>();
            }
            this.branches.add(branch);
        }
    }

    /**
     * 分支信息
     */
    public static class BranchInfo {
        private String branchId;
        private String targetNodeId;
        private String name;
        private String description;

        public BranchInfo() {}

        public BranchInfo(String branchId, String targetNodeId, String name, String description) {
            this.branchId = branchId;
            this.targetNodeId = targetNodeId;
            this.name = name;
            this.description = description;
        }

        // Getters and Setters
        public String getBranchId() { return branchId; }
        public void setBranchId(String branchId) { this.branchId = branchId; }

        public String getTargetNodeId() { return targetNodeId; }
        public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 人工审核上下文
     */
    public static class ReviewContext {
        private String reviewNodeId;
        private String reviewToken;
        private List<String> approvers;
        private String message;
        private int timeoutSeconds;
        private String timeoutAction; // REJECT, IGNORE

        public ReviewContext() {}

        // Getters and Setters
        public String getReviewNodeId() { return reviewNodeId; }
        public void setReviewNodeId(String reviewNodeId) { this.reviewNodeId = reviewNodeId; }

        public String getReviewToken() { return reviewToken; }
        public void setReviewToken(String reviewToken) { this.reviewToken = reviewToken; }

        public List<String> getApprovers() { return approvers; }
        public void setApprovers(List<String> approvers) { this.approvers = approvers; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public String getTimeoutAction() { return timeoutAction; }
        public void setTimeoutAction(String timeoutAction) { this.timeoutAction = timeoutAction; }
    }
}
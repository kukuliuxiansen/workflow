package com.openclaw.workflow.engine.smartdecompose.v2.model;

import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.ReviewStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.SubTaskStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 子任务模型
 *
 * 表示 SmartDecompose 执行过程中的一个子任务。
 * 任务可以被拆分为更小的子任务，形成树状结构。
 */
public class SubTask {

    /** 任务ID，格式：TASK_XXX */
    private String id;

    /** 父任务ID，根任务为 null */
    private String parentTaskId;

    /** 任务描述 */
    private String description;

    /** 验收标准 */
    private String criteria;

    /** 拆分深度，根任务为 0 */
    private int depth;

    /** 预估执行时间（分钟） */
    private int estimatedMinutes;

    /** 任务状态 */
    private SubTaskStatus status;

    /** 执行结果 */
    private String executionResult;

    /** 审核状态 */
    private ReviewStatus reviewStatus;

    /** 审核问题列表 */
    private List<String> reviewIssues;

    /** 重试次数 */
    private int retryCount;

    // ==================== 构造方法 ====================

    public SubTask() {
        this.status = SubTaskStatus.PENDING;
        this.reviewStatus = ReviewStatus.PENDING;
        this.reviewIssues = new ArrayList<>();
        this.retryCount = 0;
    }

    // ==================== Builder 模式 ====================

    /**
     * 创建 Builder
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        // 实现思路：创建 Builder 对象，用于链式构建 SubTask
        return new Builder();
    }

    /**
     * Builder 类
     *
     * 提供链式构建 SubTask 的能力
     */
    public static class Builder {

        private SubTask task = new SubTask();

        /**
         * 设置任务ID
         *
         * @param id 任务ID
         * @return Builder
         */
        public Builder id(String id) {
            task.id = id;
            return this;
        }

        /**
         * 设置父任务ID
         *
         * @param parentTaskId 父任务ID
         * @return Builder
         */
        public Builder parentTaskId(String parentTaskId) {
            task.parentTaskId = parentTaskId;
            return this;
        }

        /**
         * 设置任务描述
         *
         * @param description 任务描述
         * @return Builder
         */
        public Builder description(String description) {
            task.description = description;
            return this;
        }

        /**
         * 设置验收标准
         *
         * @param criteria 验收标准
         * @return Builder
         */
        public Builder criteria(String criteria) {
            task.criteria = criteria;
            return this;
        }

        /**
         * 设置拆分深度
         *
         * @param depth 深度值
         * @return Builder
         */
        public Builder depth(int depth) {
            task.depth = depth;
            return this;
        }

        /**
         * 设置预估时间
         *
         * @param minutes 预估分钟数
         * @return Builder
         */
        public Builder estimatedMinutes(int minutes) {
            task.estimatedMinutes = minutes;
            return this;
        }

        /**
         * 构建 SubTask
         *
         * 初始化默认状态后返回
         *
         * @return SubTask 实例
         */
        public SubTask build() {
            // 实现思路：确保默认值已设置，返回构建好的 task
            return task;
        }
    }

    // ==================== Getters & Setters ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public SubTaskStatus getStatus() { return status; }
    public void setStatus(SubTaskStatus status) { this.status = status; }

    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }

    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public List<String> getReviewIssues() { return reviewIssues; }
    public void setReviewIssues(List<String> reviewIssues) { this.reviewIssues = reviewIssues; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
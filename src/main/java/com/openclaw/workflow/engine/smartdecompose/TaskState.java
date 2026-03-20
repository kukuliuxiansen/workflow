package com.openclaw.workflow.engine.smartdecompose;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 任务状态
 */
public class TaskState {

    private String taskId;              // 任务ID
    private String description;         // 任务描述
    private TaskStatus status;          // 状态
    private int depth;                  // 递归深度
    private int priority;               // 优先级
    private List<TaskState> subtasks;   // 子任务列表
    private TaskState parent;           // 父任务引用
    private Object result;              // 执行结果
    private List<String> dependencies;  // 依赖任务ID
    private Date createTime;            // 创建时间
    private Date completeTime;          // 完成时间

    public enum TaskStatus {
        PENDING,        // 待处理
        RUNNING,        // 执行中
        BLOCKED,        // 被阻塞（等待依赖）
        COMPLETED,      // 已完成
        FAILED,         // 失败
        DECOMPOSED      // 已分解为子任务
    }

    public TaskState() {
        this.status = TaskStatus.PENDING;
        this.subtasks = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.createTime = new Date();
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<TaskState> getSubtasks() { return subtasks; }
    public void setSubtasks(List<TaskState> subtasks) { this.subtasks = subtasks; }

    public TaskState getParent() { return parent; }
    public void setParent(TaskState parent) { this.parent = parent; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getCompleteTime() { return completeTime; }
    public void setCompleteTime(Date completeTime) { this.completeTime = completeTime; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private TaskState task = new TaskState();

        public Builder taskId(String taskId) { task.setTaskId(taskId); return this; }
        public Builder description(String description) { task.setDescription(description); return this; }
        public Builder status(TaskStatus status) { task.setStatus(status); return this; }
        public Builder depth(int depth) { task.setDepth(depth); return this; }
        public Builder priority(int priority) { task.setPriority(priority); return this; }
        public Builder parent(TaskState parent) { task.setParent(parent); return this; }
        public Builder dependencies(List<String> dependencies) { task.setDependencies(dependencies); return this; }
        public TaskState build() { return task; }
    }
}
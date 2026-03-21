package com.openclaw.workflow.engine.smartdecompose.v2.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策响应模型
 *
 * 解析 OpenClaw 返回的决策 JSON 响应。
 * decision 可能是 "execute"（直接执行）或 "split"（拆分为子任务）。
 */
public class DecisionResponse {

    /** 决策结果：execute 或 split */
    private String decision;

    /** 分析过程 */
    private String thought;

    /** 执行结果（decision=execute 时） */
    private String result;

    /** 子任务列表（decision=split 时） */
    private List<SubTaskDef> tasks;

    // ==================== 业务方法 ====================

    /**
     * 判断是否为执行决策
     *
     * @return true 表示直接执行
     */
    public boolean isExecute() {
        // 实现思路：判断 decision 是否等于 "execute"
        return "execute".equals(decision);
    }

    /**
     * 判断是否为拆分决策
     *
     * @return true 表示需要拆分
     */
    public boolean isSplit() {
        // 实现思路：判断 decision 是否等于 "split"
        return "split".equals(decision);
    }

    // ==================== 子任务定义（JSON 映射） ====================

    /**
     * 子任务定义
     *
     * 从 JSON 中的 tasks 数组解析
     */
    public static class SubTaskDef {

        /** 任务ID */
        private String id;

        /** 任务描述 */
        private String description;

        /** 验收标准 */
        private String criteria;

        /** 预估时间（分钟） */
        private Integer estimatedMinutes;

        /**
         * 转换为 SubTask 对象
         *
         * @return SubTask 实例
         */
        public SubTask toSubTask() {
            // 实现思路：创建 SubTask，复制字段值
            SubTask task = new SubTask();
            task.setId(this.id);
            task.setDescription(this.description);
            task.setCriteria(this.criteria);
            if (this.estimatedMinutes != null) {
                task.setEstimatedMinutes(this.estimatedMinutes);
            }
            return task;
        }

        // Getters & Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCriteria() { return criteria; }
        public void setCriteria(String criteria) { this.criteria = criteria; }

        public Integer getEstimatedMinutes() { return estimatedMinutes; }
        public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    }

    // ==================== Getters & Setters ====================

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public List<SubTaskDef> getTasks() { return tasks; }
    public void setTasks(List<SubTaskDef> tasks) { this.tasks = tasks; }
}
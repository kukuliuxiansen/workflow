package com.openclaw.workflow.engine.util;

/**
 * 提示词构建公共工具类
 */
public class PromptUtils {

    private static final int MAX_OUTPUT_LENGTH = 2000;

    /**
     * 格式化输出对象
     */
    public static String formatOutput(Object output) {
        if (output == null) {
            return "(无输出)";
        }
        String str = output.toString();
        if (str.length() > MAX_OUTPUT_LENGTH) {
            return str.substring(0, MAX_OUTPUT_LENGTH) + "\n... (输出过长，已截断)";
        }
        return str;
    }

    /**
     * 分支信息
     */
    public static class BranchInfo {
        public String id;
        public String name;
        public String description;
        public String conditionDesc;  // 条件描述（给Agent看）
        public String targetNodeId;

        public BranchInfo(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public BranchInfo(String id, String name, String description, String conditionDesc) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.conditionDesc = conditionDesc;
        }
    }
}
package com.openclaw.workflow.engine.smartdecompose;

/**
 * Agent动作（从Agent输出解析）
 */
public class AgentAction {

    private DecomposeTool tool;                 // 工具类型
    private java.util.Map<String, Object> parameters;     // 参数
    private String thought;                     // 思考过程
    private int confidence;                     // 置信度 0-100

    public DecomposeTool getTool() { return tool; }
    public void setTool(DecomposeTool tool) { this.tool = tool; }

    public java.util.Map<String, Object> getParameters() { return parameters; }
    public void setParameters(java.util.Map<String, Object> parameters) { this.parameters = parameters; }

    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
}
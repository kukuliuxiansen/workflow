package com.openclaw.workflow.engine.smartdecompose;

import java.util.Map;

/**
 * 写入产物动作处理器
 */
public class WriteArtifactActionHandler implements ActionHandler {

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String name = getStringValue(params, "name", "artifact_" + System.currentTimeMillis());
        Object content = params.get("content");
        String type = getStringValue(params, "type", "text");

        if (content == null) {
            return ActionResult.error("缺少content参数");
        }

        Map<String, Object> artifactMap = new java.util.HashMap<>();
        artifactMap.put("name", name);
        artifactMap.put("content", content);
        artifactMap.put("type", type);
        artifactMap.put("timestamp", System.currentTimeMillis());

        DecomposeContext.Artifact artifact = new DecomposeContext.Artifact();
        artifact.setType(type);
        artifact.setContent(content != null ? content.toString() : "");
        artifact.setCreateTime(new java.util.Date());

        context.getArtifacts().add(artifact);

        return ActionResult.success("产物已保存: " + name, artifactMap, true);
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.WRITE_ARTIFACT;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
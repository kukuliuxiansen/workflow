package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Agent管理接口
 */
@Tag(name = "Agent", description = "Agent管理接口")
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @Value("${openclaw.config-path:}")
    private String agentConfigPath;

    @Operation(summary = "获取Agent列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> agents = loadAgents();
        return ApiResponse.success(agents);
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String id) {
        List<Map<String, Object>> agents = loadAgents();
        for (Map<String, Object> agent : agents) {
            if (agent.get("id").equals(id)) {
                return ApiResponse.success(agent);
            }
        }
        throw new RuntimeException("Agent不存在: " + id);
    }

    /**
     * 从配置文件加载Agent列表
     */
    private List<Map<String, Object>> loadAgents() {
        try {
            String configPath = agentConfigPath;
            if (configPath == null || configPath.isEmpty()) {
                configPath = System.getProperty("user.home") + "/.openclaw/openclaw-multi-agent.json";
            }
            Path path = Paths.get(configPath);
            if (!Files.exists(path)) {
                // 返回空列表而不是错误
                return new ArrayList<>();
            }

            byte[] bytes = Files.readAllBytes(path);
            String content = new String(bytes, "UTF-8");
            // 简单解析JSON (实际项目中应使用ObjectMapper)
            // 这里返回模拟数据
            return parseAgentConfig(content);
        } catch (Exception e) {
            // 配置文件读取失败，返回空列表
            return new ArrayList<>();
        }
    }

    /**
     * 解析Agent配置
     */
    private List<Map<String, Object>> parseAgentConfig(String content) {
        List<Map<String, Object>> agents = new ArrayList<>();

        try {
            // 使用简单的JSON解析
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> config = mapper.readValue(content, Map.class);

            Object agentsObj = config.get("agents");
            if (agentsObj instanceof Map) {
                Map<String, Object> agentsMap = (Map<String, Object>) agentsObj;
                for (Map.Entry<String, Object> entry : agentsMap.entrySet()) {
                    Map<String, Object> agentInfo = new HashMap<>();
                    agentInfo.put("id", entry.getKey());

                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> agentData = (Map<String, Object>) entry.getValue();
                        agentInfo.put("name", agentData.getOrDefault("name", entry.getKey()));
                        agentInfo.put("description", agentData.getOrDefault("description", ""));
                        agentInfo.put("model", agentData.getOrDefault("model", "default"));
                        agentInfo.put("systemPrompt", agentData.get("systemPrompt"));
                        agentInfo.put("tools", agentData.getOrDefault("tools", new ArrayList<>()));
                    } else {
                        agentInfo.put("name", entry.getKey());
                        agentInfo.put("description", "");
                        agentInfo.put("model", "default");
                    }
                    agents.add(agentInfo);
                }
            }
        } catch (Exception e) {
            // 解析失败返回空列表
        }

        return agents;
    }
}
package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.GlobalConfig;
import com.openclaw.workflow.repository.GlobalConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Config", description = "配置管理接口")
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final GlobalConfigRepository globalConfigRepository;

    @Value("${workflow.max-retries:3}")
    private int maxRetries;

    @Value("${workflow.max-global-retries:10}")
    private int maxGlobalRetries;

    @Value("${workflow.max-global-loop:100}")
    private int maxGlobalLoop;

    public ConfigController(GlobalConfigRepository globalConfigRepository) {
        this.globalConfigRepository = globalConfigRepository;
    }

    @Operation(summary = "获取全局配置")
    @GetMapping("/global")
    public ApiResponse<Map<String, Object>> getGlobalConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxRetries", maxRetries);
        config.put("maxGlobalRetries", maxGlobalRetries);
        config.put("maxGlobalLoop", maxGlobalLoop);

        // 从数据库加载自定义配置
        List<GlobalConfig> customConfigs = globalConfigRepository.findAll();
        for (GlobalConfig c : customConfigs) {
            config.put(c.getKey(), parseValue(c.getValue()));
        }

        return ApiResponse.success(config);
    }

    @Operation(summary = "保存全局配置")
    @PostMapping("/global")
    public ApiResponse<Void> saveGlobalConfig(@RequestBody Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());

            GlobalConfig gc = globalConfigRepository.findByKey(key)
                    .orElseGet(() -> {
                        GlobalConfig newGc = new GlobalConfig();
                        newGc.setKey(key);
                        return newGc;
                    });
            gc.setValue(value);
            globalConfigRepository.save(gc);
        }
        return ApiResponse.success();
    }

    @Operation(summary = "加载Agent列表")
    @PostMapping("/load-agents")
    public ApiResponse<List<Map<String, Object>>> loadAgents(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        List<Map<String, Object>> agents = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            return ApiResponse.success(agents);
        }

        try {
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                Map<String, Object> error = new HashMap<>();
                error.put("id", "error");
                error.put("name", "文件不存在: " + path);
                agents.add(error);
                return ApiResponse.success(agents);
            }

            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> json = mapper.readValue(content, Map.class);

            // 解析 openclaw.json 格式: { "agents": { "list": [...] } }
            if (json.containsKey("agents")) {
                Object agentsObj = json.get("agents");
                if (agentsObj instanceof Map) {
                    Map<String, Object> agentsMap = (Map<String, Object>) agentsObj;
                    if (agentsMap.containsKey("list")) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) agentsMap.get("list");
                        for (Map<String, Object> agent : list) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", agent.get("id"));
                            item.put("name", agent.get("name"));
                            agents.add(item);
                        }
                    }
                }
            } else if (json.containsKey("list")) {
                // 兼容格式: { "list": [...] }
                List<Map<String, Object>> list = (List<Map<String, Object>>) json.get("list");
                for (Map<String, Object> agent : list) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", agent.get("id"));
                    item.put("name", agent.get("name"));
                    agents.add(item);
                }
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("id", "error");
            error.put("name", "解析失败: " + e.getMessage());
            agents.add(error);
        }

        return ApiResponse.success(agents);
    }

    @Operation(summary = "加载文件内容")
    @PostMapping("/load-file")
    public ApiResponse<Map<String, Object>> loadFile(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        // TODO: 实现文件加载逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("path", path);
        result.put("content", "");
        result.put("exists", false);
        return ApiResponse.success(result);
    }

    private Object parseValue(String value) {
        if (value == null) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }
}
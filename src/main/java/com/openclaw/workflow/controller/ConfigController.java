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
    @GetMapping("/load-agents")
    public ApiResponse<List<Map<String, Object>>> loadAgents() {
        // TODO: 调用OpenClaw CLI获取agent列表
        // openclaw agent list --json
        List<Map<String, Object>> agents = new ArrayList<>();

        // 模拟数据
        Map<String, Object> agent1 = new HashMap<>();
        agent1.put("id", "agent-001");
        agent1.put("name", "Code Assistant");
        agent1.put("description", "代码编写助手");
        agent1.put("status", "active");
        agents.add(agent1);

        Map<String, Object> agent2 = new HashMap<>();
        agent2.put("id", "agent-002");
        agent2.put("name", "Code Reviewer");
        agent2.put("description", "代码审查助手");
        agent2.put("status", "active");
        agents.add(agent2);

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
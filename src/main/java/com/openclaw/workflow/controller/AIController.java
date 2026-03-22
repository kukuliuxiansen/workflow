package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.dto.GenerateWorkflowRequest;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "AI", description = "AI生成接口")
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "AI生成工作流")
    @PostMapping("/generate-workflow")
    public ApiResponse<WorkflowDto> generateWorkflow(@RequestBody GenerateWorkflowRequest request) {
        WorkflowDto workflow = aiService.generateWorkflow(request.getDescription(), request.getName());
        return ApiResponse.success(workflow);
    }

    @Operation(summary = "生成中间提示词")
    @PostMapping("/generate-intermediate-prompt")
    public ApiResponse<String> generateIntermediatePrompt(@RequestBody String requirement) {
        String prompt = aiService.generateIntermediatePrompt(requirement);
        return ApiResponse.success(prompt);
    }

    @Operation(summary = "生成最终提示词")
    @PostMapping("/generate-final-prompt")
    public ApiResponse<String> generateFinalPrompt(@RequestBody String intermediatePrompt) {
        String prompt = aiService.generateFinalPrompt(intermediatePrompt);
        return ApiResponse.success(prompt);
    }

    @Operation(summary = "生成节点提示词")
    @PostMapping("/generate-prompt")
    public ApiResponse<Map<String, String>> generatePrompt(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeInfo = (Map<String, Object>) request.get("nodeInfo");
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowContext = (Map<String, Object>) request.get("workflowContext");

        String prompt = aiService.generateNodePrompt(nodeInfo, workflowContext);

        Map<String, String> result = new HashMap<>();
        result.put("prompt", prompt != null ? prompt : "");
        return ApiResponse.success(result);
    }
}
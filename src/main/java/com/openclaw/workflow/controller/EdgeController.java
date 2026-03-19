package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.dto.CreateEdgeRequest;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.service.EdgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Edge", description = "连线管理接口")
@RestController
@RequestMapping("/api/workflows/{workflowId}/edges")
public class EdgeController {

    private final EdgeService edgeService;

    public EdgeController(EdgeService edgeService) {
        this.edgeService = edgeService;
    }

    @Operation(summary = "获取连线列表")
    @GetMapping
    public ApiResponse<List<WorkflowEdge>> list(@PathVariable String workflowId) {
        return ApiResponse.success(edgeService.findByWorkflowId(workflowId));
    }

    @Operation(summary = "创建连线")
    @PostMapping
    public ApiResponse<WorkflowEdge> create(@PathVariable String workflowId,
                                             @RequestBody CreateEdgeRequest request) {
        WorkflowEdge edge = edgeService.create(
                workflowId,
                request.getSourceNodeId(),
                request.getTargetNodeId(),
                request.getEdgeTypeEnum(),
                request.getLabel()
        );
        return ApiResponse.success(edge);
    }

    @Operation(summary = "删除连线")
    @DeleteMapping("/{edgeId}")
    public ApiResponse<Void> delete(@PathVariable String workflowId,
                                     @PathVariable String edgeId) {
        edgeService.delete(workflowId, edgeId);
        return ApiResponse.success();
    }
}
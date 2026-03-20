package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.dto.CreateNodeRequest;
import com.openclaw.workflow.dto.UpdateNodeRequest;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Node", description = "节点管理接口")
@RestController
@RequestMapping("/api/workflows/{workflowId}/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Operation(summary = "获取节点列表")
    @GetMapping
    public ApiResponse<List<WorkflowNode>> list(@PathVariable String workflowId) {
        return ApiResponse.success(nodeService.findByWorkflowId(workflowId));
    }

    @Operation(summary = "创建节点")
    @PostMapping
    public ApiResponse<WorkflowNode> create(@PathVariable String workflowId,
                                             @RequestBody CreateNodeRequest request) {
        WorkflowNode node = nodeService.create(
                workflowId,
                request.getNodeType(),
                request.getName(),
                request.getPositionX(),
                request.getPositionY(),
                request.getConfig()
        );
        return ApiResponse.success(node);
    }

    @Operation(summary = "更新节点")
    @PutMapping("/{nodeId}")
    public ApiResponse<WorkflowNode> update(@PathVariable String workflowId,
                                             @PathVariable String nodeId,
                                             @RequestBody UpdateNodeRequest request) {
        WorkflowNode node = nodeService.update(
                workflowId,
                nodeId,
                request.getName(),
                request.getPositionX(),
                request.getPositionY(),
                request.getConfig()
        );
        return ApiResponse.success(node);
    }

    @Operation(summary = "更新节点位置")
    @PatchMapping("/{nodeId}/position")
    public ApiResponse<WorkflowNode> updatePosition(@PathVariable String workflowId,
                                             @PathVariable String nodeId,
                                             @RequestBody java.util.Map<String, Integer> position) {
        nodeService.updatePosition(workflowId, nodeId, position.get("x"), position.get("y"));
        return ApiResponse.success(nodeService.findById(workflowId, nodeId));
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/{nodeId}")
    public ApiResponse<Void> delete(@PathVariable String workflowId,
                                     @PathVariable String nodeId) {
        nodeService.delete(workflowId, nodeId);
        return ApiResponse.success();
    }
}
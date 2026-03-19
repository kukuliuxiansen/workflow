package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.*;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workflow", description = "工作流管理接口")
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;

    public WorkflowController(WorkflowService workflowService,
                              WorkflowNodeRepository nodeRepository,
                              WorkflowEdgeRepository edgeRepository) {
        this.workflowService = workflowService;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Operation(summary = "获取工作流列表")
    @GetMapping
    public ApiResponse<List<Workflow>> list(
            @RequestParam(required = false) String folderId) {
        List<Workflow> workflows;
        if (folderId != null) {
            workflows = workflowService.findByFolderId(folderId);
        } else {
            workflows = workflowService.findAll();
        }
        return ApiResponse.success(workflows);
    }

    @Operation(summary = "获取工作流详情")
    @GetMapping("/{id}")
    public ApiResponse<WorkflowDto> get(@PathVariable String id) {
        Workflow workflow = workflowService.findById(id);
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(id);
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowId(id);

        WorkflowDto dto = new WorkflowDto();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setVersion(workflow.getVersion());
        dto.setStatus(workflow.getStatus());
        dto.setFolderId(workflow.getFolderId());
        dto.setGlobalConfig(workflow.getGlobalConfig());
        dto.setNodes(nodes);
        dto.setEdges(edges);

        return ApiResponse.success(dto);
    }

    @Operation(summary = "创建工作流")
    @PostMapping
    public ApiResponse<Workflow> create(@RequestBody CreateWorkflowRequest request) {
        Workflow workflow = workflowService.create(request.getName(), request.getDescription(), request.getFolderId());
        return ApiResponse.success(workflow);
    }

    @Operation(summary = "更新工作流")
    @PutMapping("/{id}")
    public ApiResponse<Workflow> update(@PathVariable String id,
                                         @RequestBody UpdateWorkflowRequest request) {
        Workflow workflow = workflowService.update(id, request.getName(),
                request.getDescription(), request.getGlobalConfig());
        return ApiResponse.success(workflow);
    }

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "克隆工作流")
    @PostMapping("/{id}/clone")
    public ApiResponse<Workflow> clone(@PathVariable String id) {
        Workflow workflow = workflowService.clone(id);
        return ApiResponse.success(workflow);
    }

    @Operation(summary = "移动工作流")
    @PutMapping("/{id}/move")
    public ApiResponse<Void> move(@PathVariable String id,
                                   @RequestBody MoveWorkflowRequest request) {
        workflowService.move(id, request.getTargetFolderId());
        return ApiResponse.success();
    }

    @Operation(summary = "导出工作流YAML")
    @GetMapping("/{id}/export")
    public ApiResponse<String> exportYaml(@PathVariable String id) {
        String yaml = workflowService.exportYaml(id);
        return ApiResponse.success(yaml);
    }

    @Operation(summary = "验证工作流")
    @GetMapping("/{id}/validate")
    public ApiResponse<java.util.Map<String, Object>> validate(@PathVariable String id) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(id);
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowId(id);

        List<String> errors = new java.util.ArrayList<>();

        // 检查是否有开始节点
        boolean hasStart = nodes.stream().anyMatch(n -> n.getType() == WorkflowNode.NodeType.START);
        if (!hasStart) {
            errors.add("工作流缺少开始节点");
        }

        // 检查是否有结束节点
        boolean hasFinish = nodes.stream().anyMatch(n -> n.getType() == WorkflowNode.NodeType.FINISH);
        if (!hasFinish) {
            errors.add("工作流缺少结束节点");
        }

        // 检查孤立节点
        for (WorkflowNode node : nodes) {
            if (node.getType() != WorkflowNode.NodeType.START && node.getType() != WorkflowNode.NodeType.FINISH) {
                boolean hasIncoming = edges.stream().anyMatch(e -> e.getTargetNodeId().equals(node.getId()));
                boolean hasOutgoing = edges.stream().anyMatch(e -> e.getSourceNodeId().equals(node.getId()));
                if (!hasIncoming && !hasOutgoing) {
                    errors.add("节点 [" + node.getName() + "] 是孤立节点");
                }
            }
        }

        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("nodeCount", nodes.size());
        result.put("edgeCount", edges.size());

        return ApiResponse.success(result);
    }
}
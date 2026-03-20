package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import com.openclaw.workflow.service.ai.AIClient;
import com.openclaw.workflow.service.ai.AIPromptBuilder;
import com.openclaw.workflow.service.ai.AIResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.base-url:https://coding.dashscope.aliyuncs.com/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen3-max-2026-01-23}")
    private String model;

    @Value("${openclaw.gateway-url:http://localhost:18789}")
    private String gatewayUrl;

    @Value("${openclaw.gateway-token:56b640cc2d91411f63255af68355c19ee33c88ec458878ca}")
    private String gatewayToken;

    @Value("${openclaw.ai-agent-id:project-manager}")
    private String aiAgentId;

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private OperationLogService logService;

    public AIService(WorkflowRepository workflowRepository,
                     WorkflowNodeRepository nodeRepository,
                     WorkflowEdgeRepository edgeRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Autowired
    public void setLogService(OperationLogService logService) {
        this.logService = logService;
    }

    public WorkflowDto generateWorkflow(String description, String name) {
        try {
            AIClient client = createAIClient();
            if (!client.isConfigured()) {
                logger.warn("未配置AI API Key或Gateway URL，使用默认模板");
                return createFallbackWorkflow(description, name);
            }

            String systemPrompt = AIPromptBuilder.buildWorkflowSystemPrompt();
            String userPrompt = AIPromptBuilder.buildWorkflowUserPrompt(description, name);
            String response = client.call(systemPrompt, userPrompt);

            return parseWorkflowResponse(response, name, description);
        } catch (Exception e) {
            logger.error("AI生成工作流失败: {}", e.getMessage());
            return createFallbackWorkflow(description, name);
        }
    }

    public String generateIntermediatePrompt(String requirement) {
        try {
            AIClient client = createAIClient();
            if (!client.isConfigured()) return null;

            String systemPrompt = AIPromptBuilder.buildIntermediateSystemPrompt();
            String userPrompt = AIPromptBuilder.buildIntermediateUserPrompt(requirement);
            return client.call(systemPrompt, userPrompt);
        } catch (Exception e) {
            logger.error("生成中间提示词失败: {}", e.getMessage());
            return null;
        }
    }

    public String generateFinalPrompt(String intermediatePrompt) {
        try {
            AIClient client = createAIClient();
            if (!client.isConfigured()) return null;

            String systemPrompt = AIPromptBuilder.buildFinalSystemPrompt();
            String userPrompt = AIPromptBuilder.buildFinalUserPrompt(intermediatePrompt);
            return client.call(systemPrompt, userPrompt);
        } catch (Exception e) {
            logger.error("生成最终提示词失败: {}", e.getMessage());
            return null;
        }
    }

    private AIClient createAIClient() {
        return new AIClient(baseUrl, apiKey, model, gatewayUrl, gatewayToken, aiAgentId);
    }

    @SuppressWarnings("unchecked")
    private WorkflowDto parseWorkflowResponse(String response, String name, String description) {
        try {
            logger.info("AI返回原始内容长度: {}", response.length());
            String jsonStr = AIResponseParser.extractJson(response);
            logger.info("解析JSON长度: {}", jsonStr.length());

            Map<String, Object> data = objectMapper.readValue(jsonStr, Map.class);

            WorkflowDto workflow = new WorkflowDto();
            workflow.setId("wf_" + System.currentTimeMillis());
            workflow.setName(name != null ? name : (String) data.getOrDefault("name", "AI生成的工作流"));
            workflow.setDescription((String) data.getOrDefault("description", description));
            workflow.setStatus(Workflow.WorkflowStatus.DRAFT);

            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) data.get("nodes");
            List<WorkflowNode> nodes = new ArrayList<>();
            List<WorkflowEdge> edges = new ArrayList<>();

            if (nodesData != null) {
                int xPos = 100;
                for (Map<String, Object> nodeData : nodesData) {
                    WorkflowNode node = new WorkflowNode();
                    String nodeId = (String) nodeData.getOrDefault("id", "node_" + UUID.randomUUID().toString().substring(0, 8));
                    node.setId(nodeId);

                    String nodeName = (String) nodeData.get("name");
                    if (nodeName == null) nodeName = (String) nodeData.get("nodeName");
                    if (nodeName == null) nodeName = AIResponseParser.generateFriendlyName(nodeId);
                    node.setName(nodeName);

                    node.setType(AIResponseParser.parseNodeType((String) nodeData.get("type")));

                    Object posX = nodeData.get("position_x");
                    Object posY = nodeData.get("position_y");
                    node.setPositionX(posX instanceof Number ? ((Number) posX).intValue() : xPos);
                    node.setPositionY(posY instanceof Number ? ((Number) posY).intValue() : 150);

                    nodes.add(node);
                    xPos += 250;

                    // 解析边
                    edges.addAll(parseEdgesFromNode(nodeData, nodeId));
                }
            }

            // 解析独立的边数据
            List<Map<String, Object>> edgesData = (List<Map<String, Object>>) data.get("edges");
            if (edgesData != null) {
                for (Map<String, Object> edgeData : edgesData) {
                    WorkflowEdge edge = new WorkflowEdge();
                    edge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                    edge.setSourceNodeId((String) edgeData.get("source"));
                    edge.setTargetNodeId((String) edgeData.get("target"));
                    String edgeType = (String) edgeData.get("type");
                    edge.setEdgeType("fail".equalsIgnoreCase(edgeType) ?
                            WorkflowEdge.EdgeType.FAIL : WorkflowEdge.EdgeType.SUCCESS);
                    edges.add(edge);
                }
            }

            logger.info("解析完成，节点数: {}, 边数: {}", nodes.size(), edges.size());
            workflow.setNodes(nodes);
            workflow.setEdges(edges);
            return workflow;

        } catch (Exception e) {
            logger.error("解析AI响应失败: {}", e.getMessage());
            return createFallbackWorkflow(description, name);
        }
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowEdge> parseEdgesFromNode(Map<String, Object> nodeData, String nodeId) {
        List<WorkflowEdge> edges = new ArrayList<>();

        // 条件节点的分支
        String trueNext = (String) nodeData.get("trueNext");
        if (trueNext == null) trueNext = (String) nodeData.get("true_next");
        if (trueNext != null) {
            WorkflowEdge trueEdge = new WorkflowEdge();
            trueEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
            trueEdge.setSourceNodeId(nodeId);
            trueEdge.setTargetNodeId(trueNext);
            trueEdge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
            edges.add(trueEdge);
        }

        String falseNext = (String) nodeData.get("falseNext");
        if (falseNext == null) falseNext = (String) nodeData.get("false_next");
        if (falseNext != null) {
            WorkflowEdge falseEdge = new WorkflowEdge();
            falseEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
            falseEdge.setSourceNodeId(nodeId);
            falseEdge.setTargetNodeId(falseNext);
            falseEdge.setEdgeType(WorkflowEdge.EdgeType.FAIL);
            edges.add(falseEdge);
        }

        // 普通节点的下一个节点
        String nextNode = (String) nodeData.get("next");
        if (nextNode == null) nextNode = (String) nodeData.get("nextNode");
        if (nextNode != null) {
            WorkflowEdge edge = new WorkflowEdge();
            edge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
            edge.setSourceNodeId(nodeId);
            edge.setTargetNodeId(nextNode);
            edge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
            edges.add(edge);
        }

        return edges;
    }

    private WorkflowDto createFallbackWorkflow(String description, String name) {
        WorkflowDto workflow = new WorkflowDto();
        workflow.setId("wf_" + System.currentTimeMillis());
        workflow.setName(name != null ? name : "AI生成的工作流");
        workflow.setDescription(description);
        workflow.setStatus(Workflow.WorkflowStatus.DRAFT);

        List<WorkflowNode> nodes = new ArrayList<>();

        WorkflowNode startNode = new WorkflowNode();
        startNode.setId("start");
        startNode.setType(WorkflowNode.NodeType.START);
        startNode.setName("开始");
        startNode.setPositionX(100);
        startNode.setPositionY(150);
        nodes.add(startNode);

        WorkflowNode agentNode = new WorkflowNode();
        agentNode.setId("node_1");
        agentNode.setType(WorkflowNode.NodeType.AGENT_EXECUTION);
        agentNode.setName("执行任务");
        agentNode.setPositionX(350);
        agentNode.setPositionY(150);
        nodes.add(agentNode);

        WorkflowNode finishNode = new WorkflowNode();
        finishNode.setId("finish");
        finishNode.setType(WorkflowNode.NodeType.FINISH);
        finishNode.setName("结束");
        finishNode.setPositionX(600);
        finishNode.setPositionY(150);
        nodes.add(finishNode);

        workflow.setNodes(nodes);

        List<WorkflowEdge> edges = new ArrayList<>();
        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setId("edge_1");
        edge1.setSourceNodeId("start");
        edge1.setTargetNodeId("node_1");
        edge1.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
        edges.add(edge1);

        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setId("edge_2");
        edge2.setSourceNodeId("node_1");
        edge2.setTargetNodeId("finish");
        edge2.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
        edges.add(edge2);

        workflow.setEdges(edges);
        return workflow;
    }
}
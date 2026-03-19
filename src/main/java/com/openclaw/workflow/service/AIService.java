package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final HttpClient httpClient;

    public AIService(WorkflowRepository workflowRepository,
                     WorkflowNodeRepository nodeRepository,
                     WorkflowEdgeRepository edgeRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public WorkflowDto generateWorkflow(String description, String name) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(description, name);

            String response = callAI(systemPrompt, userPrompt);
            return parseWorkflowResponse(response, name, description);

        } catch (Exception e) {
            logger.error("AI生成工作流失败: {}", e.getMessage(), e);
            return createFallbackWorkflow(description, name);
        }
    }

    public String generateIntermediatePrompt(String requirement) {
        try {
            String systemPrompt = "你是一个工作流节点提示词设计专家。生成简洁明了的中间提示词草案。";
            String userPrompt = "需求：" + requirement + "\n\n请生成中间提示词草案：";
            return callAI(systemPrompt, userPrompt);
        } catch (Exception e) {
            return "任务目标: " + requirement;
        }
    }

    public String generateFinalPrompt(String intermediatePrompt) {
        try {
            String systemPrompt = "你是一个工作流节点执行提示词专家。生成完整、详细的可执行提示词。";
            String userPrompt = "中间提示词:\n" + intermediatePrompt + "\n\n请生成最终执行提示词：";
            return callAI(systemPrompt, userPrompt);
        } catch (Exception e) {
            return intermediatePrompt;
        }
    }

    private String buildSystemPrompt() {
        return "你是一个工作流设计专家。用户会描述他们的需求，你需要生成一个工作流 JSON 定义。\n\n" +
                "工作流支持的节点类型：\n" +
                "1. start - 开始节点（必须有，ID为\"start\"）\n" +
                "2. agent_execution - Agent执行节点，需要 agentId 和 prompt\n" +
                "3. api_call - API调用节点，需要 url 和 method\n" +
                "4. condition - 条件判断节点\n" +
                "5. human_review - 人工审核节点\n" +
                "6. finish - 结束节点（必须有，ID为\"finish\"）\n\n" +
                "请直接返回 JSON 格式的工作流定义，不要有任何其他文字。";
    }

    private String buildUserPrompt(String description, String name) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下需求生成工作流定义：\n\n");
        prompt.append("需求描述：").append(description).append("\n\n");
        if (name != null && !name.isEmpty()) {
            prompt.append("工作流名称：").append(name).append("\n\n");
        }
        prompt.append("请生成 JSON 格式的工作流定义：");
        return prompt.toString();
    }

    private String callAI(String systemPrompt, String userPrompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("AI API Key 未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 4096);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI API 调用失败: " + response.statusCode());
        }

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }

        throw new RuntimeException("AI 返回内容为空");
    }

    @SuppressWarnings("unchecked")
    private WorkflowDto parseWorkflowResponse(String response, String name, String description) {
        try {
            // 提取JSON
            String jsonStr = response;
            if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.lastIndexOf("```");
                jsonStr = response.substring(start, end).trim();
                if (jsonStr.startsWith("json")) {
                    jsonStr = jsonStr.substring(4).trim();
                }
            }

            Map<String, Object> data = objectMapper.readValue(jsonStr, Map.class);

            WorkflowDto workflow = new WorkflowDto();
            workflow.setId("wf_" + System.currentTimeMillis());
            workflow.setName(name != null ? name : (String) data.getOrDefault("name", "AI生成的工作流"));
            workflow.setDescription((String) data.getOrDefault("description", description));
            workflow.setStatus(Workflow.WorkflowStatus.DRAFT);

            // 解析节点
            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) data.get("nodes");
            List<WorkflowNode> nodes = new ArrayList<>();
            if (nodesData != null) {
                int xPos = 100;
                for (Map<String, Object> nodeData : nodesData) {
                    WorkflowNode node = new WorkflowNode();
                    node.setId((String) nodeData.getOrDefault("id", "node_" + UUID.randomUUID().toString().substring(0, 8)));
                    node.setName((String) nodeData.getOrDefault("name", "节点"));
                    node.setType(parseNodeType((String) nodeData.get("type")));
                    node.setPositionX((Integer) nodeData.getOrDefault("position_x", xPos));
                    node.setPositionY((Integer) nodeData.getOrDefault("position_y", 150));
                    nodes.add(node);
                    xPos += 250;
                }
            }

            // 确保有开始和结束节点
            if (nodes.stream().noneMatch(n -> "start".equals(n.getId()))) {
                WorkflowNode startNode = new WorkflowNode();
                startNode.setId("start");
                startNode.setType(WorkflowNode.NodeType.START);
                startNode.setName("开始");
                startNode.setPositionX(100);
                startNode.setPositionY(150);
                nodes.add(0, startNode);
            }
            if (nodes.stream().noneMatch(n -> "finish".equals(n.getId()))) {
                WorkflowNode finishNode = new WorkflowNode();
                finishNode.setId("finish");
                finishNode.setType(WorkflowNode.NodeType.FINISH);
                finishNode.setName("结束");
                finishNode.setPositionX(100 + nodes.size() * 250);
                finishNode.setPositionY(150);
                nodes.add(finishNode);
            }

            workflow.setNodes(nodes);

            // 解析边
            List<Map<String, Object>> edgesData = (List<Map<String, Object>>) data.get("edges");
            List<WorkflowEdge> edges = new ArrayList<>();
            if (edgesData != null) {
                for (Map<String, Object> edgeData : edgesData) {
                    WorkflowEdge edge = new WorkflowEdge();
                    edge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                    edge.setSourceNodeId((String) edgeData.get("source"));
                    edge.setTargetNodeId((String) edgeData.get("target"));
                    edge.setEdgeType("fail".equals(edgeData.get("type")) ?
                            WorkflowEdge.EdgeType.FAIL : WorkflowEdge.EdgeType.SUCCESS);
                    edges.add(edge);
                }
            }
            workflow.setEdges(edges);

            return workflow;

        } catch (Exception e) {
            logger.error("解析AI响应失败: {}", e.getMessage());
            return createFallbackWorkflow(description, name);
        }
    }

    private WorkflowNode.NodeType parseNodeType(String type) {
        if (type == null) return WorkflowNode.NodeType.AGENT_EXECUTION;
        switch (type.toLowerCase()) {
            case "start": return WorkflowNode.NodeType.START;
            case "finish": return WorkflowNode.NodeType.FINISH;
            case "api_call": return WorkflowNode.NodeType.API_CALL;
            case "condition": return WorkflowNode.NodeType.CONDITION;
            case "human_review": return WorkflowNode.NodeType.HUMAN_REVIEW;
            default: return WorkflowNode.NodeType.AGENT_EXECUTION;
        }
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
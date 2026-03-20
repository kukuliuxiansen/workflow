package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
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

    // Gateway API配置（用于AI生成）
    @Value("${openclaw.gateway-url:http://localhost:18789}")
    private String gatewayUrl;

    @Value("${openclaw.gateway-token:56b640cc2d91411f63255af68355c19ee33c88ec458878ca}")
    private String gatewayToken;

    // AI生成使用的Agent ID
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
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(description, name);

            // 优先使用Gateway API
            String response;
            if (gatewayUrl != null && !gatewayUrl.isEmpty()) {
                response = callViaGateway(systemPrompt, userPrompt);
            } else if (apiKey != null && !apiKey.isEmpty()) {
                response = callViaDirectApi(systemPrompt, userPrompt);
            } else {
                logger.warn("未配置AI API Key或Gateway URL，使用默认模板");
                return createFallbackWorkflow(description, name);
            }

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
            return callAIInternal(systemPrompt, userPrompt);
        } catch (Exception e) {
            return "任务目标: " + requirement;
        }
    }

    public String generateFinalPrompt(String intermediatePrompt) {
        try {
            String systemPrompt = "你是一个工作流节点执行提示词专家。生成完整、详细的可执行提示词。";
            String userPrompt = "中间提示词:\n" + intermediatePrompt + "\n\n请生成最终执行提示词：";
            return callAIInternal(systemPrompt, userPrompt);
        } catch (Exception e) {
            return intermediatePrompt;
        }
    }

    /**
     * 内部调用AI方法
     */
    private String callAIInternal(String systemPrompt, String userPrompt) throws Exception {
        if (gatewayUrl != null && !gatewayUrl.isEmpty()) {
            return callViaGateway(systemPrompt, userPrompt);
        } else if (apiKey != null && !apiKey.isEmpty()) {
            return callViaDirectApi(systemPrompt, userPrompt);
        } else {
            throw new RuntimeException("未配置AI API Key或Gateway URL");
        }
    }

    /**
     * 通过Gateway API调用AI
     */
    private String callViaGateway(String systemPrompt, String userPrompt) throws Exception {
        logger.info("通过Gateway API调用AI: {}", gatewayUrl);

        // 记录AI输入日志
        String inputLog = "SystemPrompt: " + truncate(systemPrompt, 500) + "\nUserPrompt: " + truncate(userPrompt, 1000);
        if (logService != null) {
            logService.logOperation("AI_INPUT", "generate_workflow", inputLog, null, "AgentId: " + aiAgentId);
        }

        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(aiAgentId)
                .systemPrompt(systemPrompt)
                .message(userPrompt)
                .context("workflow_gen")
                .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            String error = "Gateway API调用失败: " + response.getErrorMessage();
            if (logService != null) {
                logService.logError("AI", "generate_workflow", error, null);
            }
            throw new RuntimeException(error);
        }

        // 记录AI输出日志（只记录payloads部分）
        String content = response.getContent();
        String outputLog = truncate(content, 2000);
        if (logService != null) {
            logService.logOperation("AI_OUTPUT", "generate_workflow", null, outputLog,
                    "Tokens: " + response.getTotalTokens());
        }

        logger.info("Gateway API调用成功，返回内容长度: {}", content.length());
        return content;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    /**
     * 通过直接API调用AI（阿里云DashScope）
     */
    private String callViaDirectApi(String systemPrompt, String userPrompt) throws Exception {
        logger.info("通过直接API调用AI: {}", baseUrl);

        java.net.HttpURLConnection connection = null;
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4096);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            java.net.URL url = new java.net.URL(baseUrl + "/chat/completions");
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    throw new RuntimeException("AI API调用失败: " + statusCode + " - " + error);
                }
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }

                Map<String, Object> responseMap = objectMapper.readValue(responseBody.toString(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }

                throw new RuntimeException("AI返回内容为空");
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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

    @SuppressWarnings("unchecked")
    private WorkflowDto parseWorkflowResponse(String response, String name, String description) {
        try {
            logger.info("AI返回原始内容: {}", response);

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

            logger.info("解析JSON: {}", jsonStr);

            Map<String, Object> data = objectMapper.readValue(jsonStr, Map.class);

            WorkflowDto workflow = new WorkflowDto();
            workflow.setId("wf_" + System.currentTimeMillis());
            workflow.setName(name != null ? name : (String) data.getOrDefault("name", "AI生成的工作流"));
            workflow.setDescription((String) data.getOrDefault("description", description));
            workflow.setStatus(Workflow.WorkflowStatus.DRAFT);

            // 解析节点
            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) data.get("nodes");
            List<WorkflowNode> nodes = new ArrayList<>();
            List<WorkflowEdge> edgesFromNodes = new ArrayList<>();  // 从节点中提取的边

            if (nodesData != null) {
                int xPos = 100;
                for (Map<String, Object> nodeData : nodesData) {
                    logger.info("解析节点数据: {}", nodeData);

                    WorkflowNode node = new WorkflowNode();
                    String nodeId = (String) nodeData.getOrDefault("id", "node_" + UUID.randomUUID().toString().substring(0, 8));
                    node.setId(nodeId);

                    // 尝试多个可能的名称字段
                    String nodeName = (String) nodeData.get("name");
                    if (nodeName == null) nodeName = (String) nodeData.get("nodeName");
                    if (nodeName == null) nodeName = (String) nodeData.get("label");
                    // 如果没有名称，从ID生成友好的名称（如 validate_order -> 验证订单）
                    if (nodeName == null) nodeName = generateFriendlyName(nodeId);
                    node.setName(nodeName);

                    node.setType(parseNodeType((String) nodeData.get("type")));

                    // 位置可能是Integer或Number类型
                    Object posX = nodeData.get("position_x");
                    Object posY = nodeData.get("position_y");
                    node.setPositionX(posX instanceof Number ? ((Number) posX).intValue() : xPos);
                    node.setPositionY(posY instanceof Number ? ((Number) posY).intValue() : 150);

                    nodes.add(node);
                    xPos += 250;

                    // 从节点配置中提取边信息
                    // 支持 trueNext/true_next 格式（条件节点）
                    String trueNext = (String) nodeData.get("trueNext");
                    if (trueNext == null) trueNext = (String) nodeData.get("true_next");
                    if (trueNext != null) {
                        WorkflowEdge trueEdge = new WorkflowEdge();
                        trueEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                        trueEdge.setSourceNodeId(nodeId);
                        trueEdge.setTargetNodeId(trueNext);
                        trueEdge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
                        edgesFromNodes.add(trueEdge);
                    }

                    String falseNext = (String) nodeData.get("falseNext");
                    if (falseNext == null) falseNext = (String) nodeData.get("false_next");
                    if (falseNext != null) {
                        WorkflowEdge falseEdge = new WorkflowEdge();
                        falseEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                        falseEdge.setSourceNodeId(nodeId);
                        falseEdge.setTargetNodeId(falseNext);
                        falseEdge.setEdgeType(WorkflowEdge.EdgeType.FAIL);
                        edgesFromNodes.add(falseEdge);
                    }

                    // 支持 next 格式（普通节点）
                    String next = (String) nodeData.get("next");
                    if (next != null && trueNext == null && falseNext == null) {
                        WorkflowEdge nextEdge = new WorkflowEdge();
                        nextEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                        nextEdge.setSourceNodeId(nodeId);
                        nextEdge.setTargetNodeId(next);
                        nextEdge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
                        edgesFromNodes.add(nextEdge);
                    }

                    // 支持 outputs 格式（AI返回的格式）
                    // outputs可以是数组 ["node1", "node2"] 或对象 {true: "node1", false: "node2"}
                    Object outputs = nodeData.get("outputs");
                    if (outputs instanceof List) {
                        // 数组格式：顺序连接到下一个节点
                        List<String> outputList = (List<String>) outputs;
                        for (int i = 0; i < outputList.size(); i++) {
                            String targetId = outputList.get(i);
                            if (targetId != null && !targetId.isEmpty()) {
                                WorkflowEdge outputEdge = new WorkflowEdge();
                                outputEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                                outputEdge.setSourceNodeId(nodeId);
                                outputEdge.setTargetNodeId(targetId);
                                outputEdge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
                                edgesFromNodes.add(outputEdge);
                            }
                        }
                    } else if (outputs instanceof Map) {
                        // 对象格式：条件分支 {true: "node1", false: "node2"}
                        Map<String, Object> outputMap = (Map<String, Object>) outputs;
                        for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
                            String condition = entry.getKey();
                            String targetId = entry.getValue() != null ? entry.getValue().toString() : null;
                            if (targetId != null && !targetId.isEmpty()) {
                                WorkflowEdge outputEdge = new WorkflowEdge();
                                outputEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                                outputEdge.setSourceNodeId(nodeId);
                                outputEdge.setTargetNodeId(targetId);
                                // true条件是SUCCESS，其他是FAIL
                                outputEdge.setEdgeType("true".equalsIgnoreCase(condition) ?
                                        WorkflowEdge.EdgeType.SUCCESS : WorkflowEdge.EdgeType.FAIL);
                                edgesFromNodes.add(outputEdge);
                            }
                        }
                    }

                    // 支持 conditions 数组格式（条件节点）
                    // conditions: [{expression: "...", next: "node1"}, ...]
                    Object conditions = nodeData.get("conditions");
                    if (conditions instanceof List) {
                        List<Map<String, Object>> conditionList = (List<Map<String, Object>>) conditions;
                        for (Map<String, Object> cond : conditionList) {
                            String targetId = (String) cond.get("next");
                            if (targetId != null && !targetId.isEmpty()) {
                                WorkflowEdge condEdge = new WorkflowEdge();
                                condEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                                condEdge.setSourceNodeId(nodeId);
                                condEdge.setTargetNodeId(targetId);
                                condEdge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
                                edgesFromNodes.add(condEdge);
                            }
                        }
                    }

                    // 支持 defaultNext 字段（条件节点的默认分支）
                    String defaultNext = (String) nodeData.get("defaultNext");
                    if (defaultNext == null) defaultNext = (String) nodeData.get("default_next");
                    if (defaultNext != null && !defaultNext.isEmpty()) {
                        WorkflowEdge defaultEdge = new WorkflowEdge();
                        defaultEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
                        defaultEdge.setSourceNodeId(nodeId);
                        defaultEdge.setTargetNodeId(defaultNext);
                        defaultEdge.setEdgeType(WorkflowEdge.EdgeType.FAIL);
                        edgesFromNodes.add(defaultEdge);
                    }
                }
            }

            // 确保有开始节点（只有ID为start的才算开始节点）
            if (nodes.stream().noneMatch(n -> "start".equals(n.getId()))) {
                WorkflowNode startNode = new WorkflowNode();
                startNode.setId("start");
                startNode.setType(WorkflowNode.NodeType.START);
                startNode.setName("开始");
                startNode.setPositionX(100);
                startNode.setPositionY(150);
                nodes.add(0, startNode);
            }

            // 只有当没有任何finish类型的节点时才添加默认结束节点
            boolean hasFinishNode = nodes.stream().anyMatch(n ->
                    n.getType() == WorkflowNode.NodeType.FINISH ||
                    "finish".equalsIgnoreCase(n.getId()));
            if (!hasFinishNode) {
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
            List<WorkflowEdge> edges = new ArrayList<>();

            // 首先添加从节点中提取的边
            edges.addAll(edgesFromNodes);

            // 然后解析独立的edges数组
            List<Map<String, Object>> edgesData = (List<Map<String, Object>>) data.get("edges");
            if (edgesData != null) {
                logger.info("解析边数据: {}", edgesData);
                for (Map<String, Object> edgeData : edgesData) {
                    WorkflowEdge edge = new WorkflowEdge();
                    edge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));

                    // 兼容多种字段名
                    String source = (String) edgeData.get("source");
                    if (source == null) source = (String) edgeData.get("sourceNodeId");
                    if (source == null) source = (String) edgeData.get("from");

                    String target = (String) edgeData.get("target");
                    if (target == null) target = (String) edgeData.get("targetNodeId");
                    if (target == null) target = (String) edgeData.get("to");

                    if (source == null || target == null) {
                        logger.warn("边缺少source或target: {}", edgeData);
                        continue;
                    }

                    edge.setSourceNodeId(source);
                    edge.setTargetNodeId(target);

                    String edgeType = (String) edgeData.get("type");
                    edge.setEdgeType("fail".equalsIgnoreCase(edgeType) ?
                            WorkflowEdge.EdgeType.FAIL : WorkflowEdge.EdgeType.SUCCESS);
                    edges.add(edge);
                }
            }
            logger.info("解析完成，节点数: {}, 边数: {}", nodes.size(), edges.size());
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

    /**
     * 从节点ID生成友好的中文名称
     * 如: validate_order -> 验证订单, check_stock -> 检查库存
     */
    private String generateFriendlyName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) return "节点";

        // 特殊节点
        if ("start".equalsIgnoreCase(nodeId)) return "开始";
        if ("finish".equalsIgnoreCase(nodeId)) return "结束";

        // 常见英文词汇翻译映射
        Map<String, String> wordMap = new HashMap<>();
        wordMap.put("validate", "验证");
        wordMap.put("check", "检查");
        wordMap.put("process", "处理");
        wordMap.put("create", "创建");
        wordMap.put("send", "发送");
        wordMap.put("notify", "通知");
        wordMap.put("review", "审核");
        wordMap.put("calculate", "计算");
        wordMap.put("update", "更新");
        wordMap.put("delete", "删除");
        wordMap.put("get", "获取");
        wordMap.put("post", "提交");
        wordMap.put("order", "订单");
        wordMap.put("stock", "库存");
        wordMap.put("payment", "支付");
        wordMap.put("user", "用户");
        wordMap.put("email", "邮箱");
        wordMap.put("code", "验证码");
        wordMap.put("welcome", "欢迎");
        wordMap.put("verification", "验证");
        wordMap.put("inventory", "库存");
        wordMap.put("price", "价格");
        wordMap.put("condition", "条件");
        wordMap.put("result", "结果");
        wordMap.put("success", "成功");
        wordMap.put("failed", "失败");
        wordMap.put("fail", "失败");
        wordMap.put("insufficient", "不足");
        wordMap.put("article", "文章");
        wordMap.put("content", "内容");
        wordMap.put("quality", "质量");
        wordMap.put("publish", "发布");
        wordMap.put("reject", "拒绝");
        wordMap.put("analyze", "分析");
        wordMap.put("analysis", "分析");

        // 分割ID并翻译
        String[] parts = nodeId.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            String translated = wordMap.get(part.toLowerCase());
            if (translated != null) {
                name.append(translated);
            } else if (part.length() > 0) {
                // 首字母大写
                name.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            }
        }

        return name.length() > 0 ? name.toString() : nodeId;
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
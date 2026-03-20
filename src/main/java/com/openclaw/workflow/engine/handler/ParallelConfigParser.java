package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行节点配置解析器
 */
public class ParallelConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(ParallelConfigParser.class);

    public static ParallelConfig parse(WorkflowNode node, ParallelConfig defaults) {
        ParallelConfig config = new ParallelConfig();
        config.setExecutionMode(defaults != null ? defaults.getExecutionMode() : "ALL");

        if (node.getConfig() == null) {
            return config;
        }

        JsonNode jsonConfig = NodeConfigParser.parseJson(node.getConfig());
        if (jsonConfig == null) {
            return config;
        }

        parseBasicConfig(config, jsonConfig);
        parseBranches(config, jsonConfig);
        parseDefaultBranches(config, jsonConfig);

        return config;
    }

    private static void parseBasicConfig(ParallelConfig config, JsonNode jsonConfig) {
        config.setExecutionMode(NodeConfigParser.getString(jsonConfig, "executionMode", "ALL"));
        config.setMergeNodeId(NodeConfigParser.getString(jsonConfig, "mergeNode", null));
        config.setCustomPrompt(NodeConfigParser.getString(jsonConfig, "customPrompt", null));
    }

    private static void parseBranches(ParallelConfig config, JsonNode jsonConfig) {
        JsonNode branchesNode = NodeConfigParser.getNode(jsonConfig, "branches");
        if (branchesNode == null || !branchesNode.isArray()) {
            return;
        }

        List<Branch> branches = new ArrayList<>();
        for (JsonNode branchNode : branchesNode) {
            Branch branch = new Branch();
            branch.setId(NodeConfigParser.getString(branchNode, "id", ""));
            branch.setName(NodeConfigParser.getString(branchNode, "name", ""));
            branch.setDescription(NodeConfigParser.getString(branchNode, "description", ""));
            branch.setTargetNodeId(NodeConfigParser.getString(branchNode, "targetNodeId", ""));
            branch.setConditionDesc(NodeConfigParser.getString(branchNode, "conditionDesc", null));
            branches.add(branch);
        }
        config.setBranches(branches);
    }

    private static void parseDefaultBranches(ParallelConfig config, JsonNode jsonConfig) {
        JsonNode defaultBranchesNode = NodeConfigParser.getNode(jsonConfig, "defaultBranches");
        if (defaultBranchesNode == null || !defaultBranchesNode.isArray()) {
            return;
        }

        List<String> defaultBranches = new ArrayList<>();
        for (JsonNode defaultBranch : defaultBranchesNode) {
            defaultBranches.add(defaultBranch.asText());
        }
        config.setDefaultBranches(defaultBranches);
    }
}
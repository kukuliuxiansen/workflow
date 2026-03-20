package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.service.NodePromptService;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行执行节点处理器
 *
 * 支持两种执行模式：
 * 1. ALL: 执行所有分支
 * 2. DYNAMIC: Agent选择执行哪些分支
 *
 * Agent决策协议：
 * [NODE_DECISION]
 * node_ids: branch_a, branch_b
 * reason: 选择原因
 * [/NODE_DECISION]
 *
 * 配置结构：
 * {
 *   "executionMode": "DYNAMIC",
 *   "branches": [
 *     {
 *       "id": "branch_1",
 *       "name": "分支名称",
 *       "description": "分支描述",
 *       "targetNodeId": "node_xxx",
 *       "conditionDesc": "执行条件说明（给Agent看）"
 *     }
 *   ],
 *   "mergeNode": "node_merge",
 *   "defaultBranches": ["branch_1"],
 *   "customPrompt": "自定义提示词（可选）"
 * }
 */
public class ParallelNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ParallelNodeHandler.class);

    private final ParallelBranchSelector branchSelector = new ParallelBranchSelector();

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        ParallelConfig config = ParallelConfigParser.parse(node, null);

        logger.info("执行并行节点: {} (模式: {})", node.getName(), config.getExecutionMode());

        List<Branch> branches = config.getBranches();
        if (branches.isEmpty()) {
            logger.warn("并行节点没有配置分支");
            return NodeResult.failed("并行节点没有配置分支");
        }

        List<String> selectedBranchIds = selectBranches(config, context);

        if (selectedBranchIds.isEmpty()) {
            selectedBranchIds = resolveDefaultBranches(config, branches);
        }

        NodeResult result = buildResult(branches, selectedBranchIds, config, node);
        logger.info("并行节点选择的分支: {}", selectedBranchIds);

        return result;
    }

    private List<String> selectBranches(ParallelConfig config, NodeExecutionContext context) throws Exception {
        if ("ALL".equalsIgnoreCase(config.getExecutionMode())) {
            List<String> allIds = new ArrayList<>();
            for (Branch branch : config.getBranches()) {
                allIds.add(branch.getId());
            }
            return allIds;
        }
        return branchSelector.selectBranches(config, context);
    }

    private List<String> resolveDefaultBranches(ParallelConfig config, List<Branch> branches) {
        logger.warn("没有选择任何分支，使用默认分支");
        if (config.getDefaultBranches() != null && !config.getDefaultBranches().isEmpty()) {
            return config.getDefaultBranches();
        }
        List<String> defaultIds = new ArrayList<>();
        defaultIds.add(branches.get(0).getId());
        return defaultIds;
    }

    private NodeResult buildResult(List<Branch> branches, List<String> selectedBranchIds,
                                   ParallelConfig config, WorkflowNode node) {
        List<String> targetNodeIds = new ArrayList<>();
        List<NodeResult.BranchInfo> branchInfos = new ArrayList<>();

        for (Branch branch : branches) {
            if (selectedBranchIds.contains(branch.getId())) {
                targetNodeIds.add(branch.getTargetNodeId());
                branchInfos.add(new NodeResult.BranchInfo(
                        branch.getId(),
                        branch.getTargetNodeId(),
                        branch.getName(),
                        branch.getDescription()
                ));
            }
        }

        NodeResult.ParallelContext parallelContext = new NodeResult.ParallelContext();
        parallelContext.setParallelNodeId(node.getId());
        parallelContext.setBranches(branchInfos);
        parallelContext.setMergeNodeId(config.getMergeNodeId());

        NodeResult result = NodeResult.successWithNodes(
                "并行执行 " + targetNodeIds.size() + " 个分支",
                targetNodeIds
        );
        result.setParallelContext(parallelContext);

        return result;
    }

    // ==================== 配置方法 ====================

    public void setGatewayUrl(String gatewayUrl) {
        branchSelector.setGatewayUrl(gatewayUrl);
    }

    public void setGatewayToken(String gatewayToken) {
        branchSelector.setGatewayToken(gatewayToken);
    }

    public void setDecisionAgentId(String decisionAgentId) {
        branchSelector.setDecisionAgentId(decisionAgentId);
    }

    public void setPromptService(NodePromptService promptService) {
        branchSelector.setPromptService(promptService);
    }
}
package com.openclaw.workflow.service;

import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NodeService 单元测试
 *
 * 测试节点的 CRUD 操作、位置更新、删除等功能
 */
class NodeServiceTest {

    private WorkflowRepository workflowRepository;
    private WorkflowNodeRepository nodeRepository;
    private WorkflowEdgeRepository edgeRepository;
    private NodeService nodeService;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        nodeRepository = mock(WorkflowNodeRepository.class);
        edgeRepository = mock(WorkflowEdgeRepository.class);
        nodeService = new NodeService(workflowRepository, nodeRepository, edgeRepository);
    }

    // ==================== 节点创建测试 ====================

    @Test
    @DisplayName("创建节点时应生成唯一ID")
    void testCreateNodeGeneratesId() {
        // Arrange
        when(workflowRepository.findById("wf_123")).thenReturn(Optional.of(new Workflow()));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            node.setId("node_mock_" + System.nanoTime());
            return node;
        });

        // Act
        WorkflowNode node = nodeService.create("wf_123", WorkflowNode.NodeType.AGENT_EXECUTION,
                "测试节点", 100, 200, null);

        // Assert
        assertNotNull(node);
        assertEquals("node_mock_", node.getId().substring(0, 10));
        assertEquals("测试节点", node.getName());
        assertEquals(WorkflowNode.NodeType.AGENT_EXECUTION, node.getType());
    }

    @Test
    @DisplayName("创建节点时使用默认位置")
    void testCreateNodeWithDefaultPosition() {
        // Arrange
        when(workflowRepository.findById("wf_123")).thenReturn(Optional.of(new Workflow()));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            node.setId("node_mock_" + System.nanoTime());
            return node;
        });

        // Act
        WorkflowNode node = nodeService.create("wf_123", WorkflowNode.NodeType.CONDITION,
                "条件节点", null, null, null);

        // Assert
        assertEquals(Integer.valueOf(300), node.getPositionX());
        assertEquals(Integer.valueOf(150), node.getPositionY());
    }

    @Test
    @DisplayName("创建节点时使用默认类型")
    void testCreateNodeWithDefaultType() {
        // Arrange
        when(workflowRepository.findById("wf_123")).thenReturn(Optional.of(new Workflow()));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            node.setId("node_mock_" + System.nanoTime());
            return node;
        });

        // Act
        WorkflowNode node = nodeService.create("wf_123", null, "测试节点", 100, 200, null);

        // Assert
        assertEquals(WorkflowNode.NodeType.AGENT_EXECUTION, node.getType());
    }

    @Test
    @DisplayName("创建节点时使用默认名称")
    void testCreateNodeWithDefaultName() {
        // Arrange
        when(workflowRepository.findById("wf_123")).thenReturn(Optional.of(new Workflow()));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            node.setId("node_mock_" + System.nanoTime());
            return node;
        });

        // Act
        WorkflowNode node = nodeService.create("wf_123", WorkflowNode.NodeType.FINISH, null, 100, 200, null);

        // Assert
        assertEquals("新节点", node.getName());
    }

    @Test
    @DisplayName("创建节点时工作流不存在应抛出异常")
    void testCreateNodeWithNonExistentWorkflow() {
        // Arrange
        when(workflowRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> nodeService.create("nonexistent", WorkflowNode.NodeType.START, "节点", 100, 200, null));
        assertTrue(thrown.getMessage().contains("工作流不存在"));
    }

    // ==================== 节点查询测试 ====================

    @Test
    @DisplayName("根据ID查询节点")
    void testFindNodeById() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");
        mockNode.setName("测试节点");

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));

        // Act
        WorkflowNode found = nodeService.findById("wf_123", "node_1");

        // Assert
        assertEquals("node_1", found.getId());
        assertEquals("测试节点", found.getName());
    }

    @Test
    @DisplayName("查询不存在的节点应抛出异常")
    void testFindNodeByIdNotFound() {
        // Arrange
        when(nodeRepository.findByWorkflowIdAndId("wf_123", "nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> nodeService.findById("wf_123", "nonexistent"));
        assertTrue(thrown.getMessage().contains("节点不存在"));
    }

    @Test
    @DisplayName("查询工作流的所有节点")
    void testFindByWorkflowId() {
        // Arrange
        List<WorkflowNode> mockNodes = Arrays.asList(
            createNode("node_1", "节点1"),
            createNode("node_2", "节点2"),
            createNode("node_3", "节点3")
        );
        when(nodeRepository.findByWorkflowIdOrderByCreatedAtAsc("wf_123")).thenReturn(mockNodes);

        // Act
        List<WorkflowNode> nodes = nodeService.findByWorkflowId("wf_123");

        // Assert
        assertEquals(3, nodes.size());
    }

    // ==================== 节点更新测试 ====================

    @Test
    @DisplayName("更新节点名称")
    void testUpdateNodeName() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");
        mockNode.setName("原名称");

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WorkflowNode updated = nodeService.update("wf_123", "node_1", "新名称", null, null, null);

        // Assert
        assertEquals("新名称", updated.getName());
    }

    @Test
    @DisplayName("更新节点位置")
    void testUpdateNodePosition() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");
        mockNode.setPositionX(100);
        mockNode.setPositionY(200);

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WorkflowNode updated = nodeService.update("wf_123", "node_1", null, 300, 400, null);

        // Assert
        assertEquals(Integer.valueOf(300), updated.getPositionX());
        assertEquals(Integer.valueOf(400), updated.getPositionY());
    }

    @Test
    @DisplayName("更新节点配置")
    void testUpdateNodeConfig() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");
        mockNode.setConfig("{\"old\":\"config\"}");

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WorkflowNode updated = nodeService.update("wf_123", "node_1", null, null, null, "{\"new\":\"config\"}");

        // Assert
        assertEquals("{\"new\":\"config\"}", updated.getConfig());
    }

    @Test
    @DisplayName("更新节点位置")
    void testUpdatePosition() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");
        mockNode.setPositionX(100);
        mockNode.setPositionY(200);

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        nodeService.updatePosition("wf_123", "node_1", 300, 400);

        // Assert
        verify(nodeRepository).save(argThat(node -> {
            return Integer.valueOf(300).equals(node.getPositionX()) &&
                    Integer.valueOf(400).equals(node.getPositionY());
        }));
    }

    // ==================== 节点删除测试 ====================

    @Test
    @DisplayName("删除节点应同时删除相关边")
    void testDeleteNodeRemovesEdges() {
        // Arrange
        WorkflowNode mockNode = new WorkflowNode();
        mockNode.setId("node_1");

        when(nodeRepository.findByWorkflowIdAndId("wf_123", "node_1"))
                .thenReturn(Optional.of(mockNode));

        // Act
        nodeService.delete("wf_123", "node_1");

        // Assert
        verify(edgeRepository).deleteBySourceNodeId("node_1");
        verify(edgeRepository).deleteByTargetNodeId("node_1");
        verify(nodeRepository).deleteById("node_1");
    }

    @Test
    @DisplayName("不存在的节点删除（当前实现不验证节点是否存在）")
    void testDeleteNonExistentNode() {
        // Arrange - delete 方法当前不验证节点是否存在，所以不会抛异常
        // 这是当前实现的特性

        // Act
        nodeService.delete("wf_123", "nonexistent");

        // Assert - 由于当前实现没有检查，不应该抛出异常
        // 如果需要修改为检查节点存在性，可以在这里添加测试
        verify(edgeRepository, times(1)).deleteBySourceNodeId("nonexistent");
        verify(edgeRepository, times(1)).deleteByTargetNodeId("nonexistent");
        verify(nodeRepository, times(1)).deleteById("nonexistent");
    }

    // ==================== 实体测试 ====================

    @Test
    @DisplayName("节点创建时自动设置时间戳")
    void testNodeSetsTimestampOnCreation() {
        // Arrange & Act
        WorkflowNode node = new WorkflowNode();

        // Assert
        assertNotNull(node.getCreatedAt());
        assertNotNull(node.getUpdatedAt());
    }

    @Test
    @DisplayName("节点类型枚举包括所有常见节点类型")
    void testAllNodeTypeValues() {
        // Assert - 原始 NodeType 有 11 个值
        assertEquals(11, WorkflowNode.NodeType.values().length);
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.START));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.FINISH));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.AGENT_EXECUTION));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.CONDITION));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.PARALLEL));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.LOOP));
        assertTrue(Arrays.asList(WorkflowNode.NodeType.values()).contains(WorkflowNode.NodeType.HUMAN_REVIEW));
    }

    @Test
    @DisplayName("测试 WorkflowEdge 默认边类型为 SUCCESS")
    void testEdgeDefaultType() {
        // Arrange & Act
        WorkflowEdge edge = new WorkflowEdge();

        // Assert
        assertEquals(WorkflowEdge.EdgeType.SUCCESS, edge.getEdgeType());
    }

    @Test
    @DisplayName("测试 WorkflowEdge 创建")
    void testWorkflowEdgeCreation() {
        // Arrange & Act
        WorkflowEdge edge = new WorkflowEdge();
        edge.setWorkflowId("wf_123");
        edge.setSourceNodeId("node_1");
        edge.setTargetNodeId("node_2");
        edge.setEdgeType(WorkflowEdge.EdgeType.FAIL);

        // Assert
        assertEquals("wf_123", edge.getWorkflowId());
        assertEquals("node_1", edge.getSourceNodeId());
        assertEquals("node_2", edge.getTargetNodeId());
        assertEquals(WorkflowEdge.EdgeType.FAIL, edge.getEdgeType());
    }

    // ==================== 辅助方法 ====================

    private WorkflowNode createNode(String id, String name) {
        WorkflowNode node = new WorkflowNode();
        node.setId(id);
        node.setName(name);
        return node;
    }
}

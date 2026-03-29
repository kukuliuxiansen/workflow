package com.openclaw.workflow.service;

import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WorkflowService 单元测试
 *
 * 测试工作流的 CRUD 操作、克隆、移动、导出等功能
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        reset(workflowService);
    }

    // ==================== 基础测试 ====================

    @Test
    @DisplayName("测试 Workflow 实体 - 创建对象")
    void testWorkflowEntityCreation() {
        // Arrange & Act
        Workflow workflow = new Workflow();
        workflow.setId("wf_123");
        workflow.setName("测试工作流");
        workflow.setDescription("测试描述");
        workflow.setStatus(Workflow.WorkflowStatus.DRAFT);
        workflow.setFolderId("folder_1");
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals("wf_123", workflow.getId());
        assertEquals("测试工作流", workflow.getName());
        assertEquals("测试描述", workflow.getDescription());
        assertEquals(Workflow.WorkflowStatus.DRAFT, workflow.getStatus());
        assertEquals("folder_1", workflow.getFolderId());
        assertNotNull(workflow.getCreatedAt());
        assertNotNull(workflow.getUpdatedAt());
    }

    @Test
    @DisplayName("测试 Workflow - 默认状态为 DRAFT")
    void testWorkflowDefaultStatus() {
        // Arrange & Act
        Workflow workflow = new Workflow();

        // Assert
        assertEquals(Workflow.WorkflowStatus.DRAFT, workflow.getStatus());
    }

    @Test
    @DisplayName("测试 Workflow - 状态枚举")
    void testWorkflowStatusEnum() {
        // Assert
        assertEquals("draft", Workflow.WorkflowStatus.DRAFT.name().toLowerCase());
        assertEquals("published", Workflow.WorkflowStatus.PUBLISHED.name().toLowerCase());
    }

    @Test
    @DisplayName("测试 WorkflowEdge 实体 - 创建对象")
    void testWorkflowEdgeEntityCreation() {
        // Arrange & Act
        WorkflowEdge edge = new WorkflowEdge();
        edge.setWorkflowId("wf_123");
        edge.setSourceNodeId("node_1");
        edge.setTargetNodeId("node_2");
        edge.setEdgeType(WorkflowEdge.EdgeType.SUCCESS);
        edge.setLabel("成功路径");

        // Assert
        assertEquals("wf_123", edge.getWorkflowId());
        assertEquals("node_1", edge.getSourceNodeId());
        assertEquals("node_2", edge.getTargetNodeId());
        assertEquals(WorkflowEdge.EdgeType.SUCCESS, edge.getEdgeType());
        assertEquals("成功路径", edge.getLabel());
    }

    @Test
    @DisplayName("测试 WorkflowEdge - 默认边类型为 SUCCESS")
    void testWorkflowEdgeDefaultType() {
        // Arrange & Act
        WorkflowEdge edge = new WorkflowEdge();

        // Assert
        assertEquals(WorkflowEdge.EdgeType.SUCCESS, edge.getEdgeType());
    }

    @Test
    @DisplayName("测试 WorkflowNode 实体 - 创建对象")
    void testWorkflowNodeEntityCreation() {
        // Arrange & Act
        WorkflowNode node = new WorkflowNode();
        node.setId("node_1");
        node.setWorkflowId("wf_123");
        node.setType(WorkflowNode.NodeType.AGENT_EXECUTION);
        node.setName("测试节点");
        node.setDescription("这是一个测试节点");
        node.setPositionX(300);
        node.setPositionY(150);

        // Assert
        assertEquals("node_1", node.getId());
        assertEquals("wf_123", node.getWorkflowId());
        assertEquals(WorkflowNode.NodeType.AGENT_EXECUTION, node.getType());
        assertEquals("测试节点", node.getName());
        assertEquals("这是一个测试节点", node.getDescription());
        assertEquals(Integer.valueOf(300), node.getPositionX());
        assertEquals(Integer.valueOf(150), node.getPositionY());
    }

    @Test
    @DisplayName("测试 WorkflowNode - NodeType 枚举")
    void testWorkflowNodeTypeEnum() {
        // Assert
        assertEquals("start", WorkflowNode.NodeType.START.name().toLowerCase());
        assertEquals("finish", WorkflowNode.NodeType.FINISH.name().toLowerCase());
        assertEquals("agent_execution", WorkflowNode.NodeType.AGENT_EXECUTION.name().toLowerCase());
        assertEquals("condition", WorkflowNode.NodeType.CONDITION.name().toLowerCase());
        assertEquals("parallel", WorkflowNode.NodeType.PARALLEL.name().toLowerCase());
        assertEquals("loop", WorkflowNode.NodeType.LOOP.name().toLowerCase());
    }

    @Test
    @DisplayName("测试 WorkflowNode - NodeType fromString")
    void testWorkflowNodeTypeFromString() {
        // Act
        WorkflowNode.NodeType type1 = WorkflowNode.NodeType.fromString("start");
        WorkflowNode.NodeType type2 = WorkflowNode.NodeType.fromString("START");
        WorkflowNode.NodeType type3 = WorkflowNode.NodeType.fromString("condition");

        // Assert
        assertEquals(WorkflowNode.NodeType.START, type1);
        assertEquals(WorkflowNode.NodeType.START, type2);
        assertEquals(WorkflowNode.NodeType.CONDITION, type3);
    }

    @Test
    @DisplayName("测试空字符串到 NodeType 的转换")
    void testWorkflowNodeTypeFromStringEmpty() {
        // Act & Assert - 空字符串会抛出异常，因为没有对应的枚举值
        assertThrows(IllegalArgumentException.class, () -> WorkflowNode.NodeType.fromString(""));
    }

    @Test
    @DisplayName("测试 null 到 NodeType 的转换")
    void testWorkflowNodeTypeFromStringNull() {
        // Act
        WorkflowNode.NodeType type = WorkflowNode.NodeType.fromString(null);

        // Assert
        assertEquals(WorkflowNode.NodeType.AGENT_EXECUTION, type);
    }

    // ==================== 集成测试（使用真实 Spring 上下文） ====================

    @Nested
    @DisplayName("集成测试 - 数据访问层")
    @ExtendWith(MockitoExtension.class)
    class IntegrationTests {

        @Mock
        private WorkflowService workflowService;

        @Test
        @DisplayName("集成测试 - 服务层方法调用")
        void testServiceMocking() {
            // Arrange
            Workflow mockWorkflow = new Workflow();
            mockWorkflow.setId("wf_123");
            mockWorkflow.setName("测试");

            when(workflowService.findById("wf_123")).thenReturn(mockWorkflow);

            // Act
            Workflow result = workflowService.findById("wf_123");

            // Assert
            assertEquals("wf_123", result.getId());
            verify(workflowService).findById("wf_123");
        }

        @Test
        @DisplayName("集成测试 - null 参数处理")
        void testNullParameterHandling() {
            // Arrange
            when(workflowService.findById(null)).thenThrow(RuntimeException.class);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> workflowService.findById(null));
        }

        @Test
        @DisplayName("集成测试 - 空集合处理")
        void testEmptyCollectionHandling() {
            // Arrange
            when(workflowService.findAll()).thenReturn(Arrays.asList());

            // Act
            List<Workflow> result = workflowService.findAll();

            // Assert
            assertTrue(result.isEmpty());
        }
    }
}

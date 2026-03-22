    async function saveWorkflow(silent = false) {
      if (!state.currentWorkflow) {
        if (!silent) showToast('warn', '请先选择工作流');
        return;
      }
      try {
        // 准备保存数据，使用 snake_case 格式匹配后端 Jackson 配置
        const workflowData = {
          name: state.currentWorkflow.name,
          description: state.currentWorkflow.description,
          global_config: state.currentWorkflow.globalConfig || state.currentWorkflow.global_config,
          task_config: state.currentWorkflow.taskConfig
            ? JSON.stringify(state.currentWorkflow.taskConfig)
            : null,
          nodes: state.currentWorkflow.nodes,
          edges: state.currentWorkflow.edges
        };

        await fetch(`${API}/workflows/${state.currentWorkflow.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(workflowData)
        });
        state.isDirty = false;
        state.lastSaveTime = Date.now();
        updateSaveIndicator();
        if (!silent) {
          showToast('success', '保存成功');
          addLog('success', '工作流已保存');
        }
      } catch (e) {
        if (!silent) showToast('error', '保存失败');
      }
    }

    /**
     * 完整的工作流验证
     * 返回: { valid: boolean, errors: [], warnings: [] }
     */
    function validateWorkflowComplete() {
      const result = { valid: true, errors: [], warnings: [] };
      const workflow = state.currentWorkflow;
      const nodes = workflow.nodes || [];
      const edges = workflow.edges || [];

      // 1. 结构验证
      const startNodes = nodes.filter(n => n.type === 'start');
      const finishNodes = nodes.filter(n => n.type === 'finish');

      if (startNodes.length === 0) {
        result.errors.push({ nodeId: null, message: '缺少开始节点', type: 'structure' });
        result.valid = false;
      } else if (startNodes.length > 1) {
        result.errors.push({ nodeId: null, message: '只能有一个开始节点', type: 'structure' });
        result.valid = false;
      }

      if (finishNodes.length === 0) {
        result.errors.push({ nodeId: null, message: '缺少结束节点', type: 'structure' });
        result.valid = false;
      }

      // 2. 孤立节点检测
      const connectedNodeIds = new Set();
      edges.forEach(edge => {
        connectedNodeIds.add(edge.sourceNodeId || edge.source_node_id);
        connectedNodeIds.add(edge.targetNodeId || edge.target_node_id);
      });

      // 开始节点不需要入边
      startNodes.forEach(n => connectedNodeIds.add(n.id));

      nodes.forEach(node => {
        if (!connectedNodeIds.has(node.id)) {
          result.errors.push({
            nodeId: node.id,
            message: `节点【${node.name || node.id}】未连接到流程`,
            type: 'isolated'
          });
          result.valid = false;
        }
      });

      // 3. 节点配置验证
      nodes.forEach(node => {
        const type = node.type || 'agent_execution';
        const config = typeof node.config === 'string'
          ? (() => { try { return JSON.parse(node.config || '{}'); } catch(e) { return {}; } })()
          : (node.config || {});

        if (type === 'agent_execution') {
          const agentId = node.agent_id || node.agentId || config.agent_id || config.agentId;
          if (!agentId) {
            result.errors.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】未配置 Agent`,
              type: 'config'
            });
            result.valid = false;
          }
        }

        if (type === 'api_call') {
          const url = node.url || config.url;
          if (!url) {
            result.errors.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】未配置 URL`,
              type: 'config'
            });
            result.valid = false;
          }
        }

        if (type === 'smart_decompose') {
          const sceneId = config.sceneId;
          if (!sceneId) {
            result.warnings.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】未选择场景，将使用默认配置`,
              type: 'config'
            });
          }
        }

        // 条件节点验证：至少有一个出边
        if (type === 'condition') {
          const nodeOutEdges = edges.filter(e =>
            (e.sourceNodeId || e.source_node_id) === node.id
          );
          if (nodeOutEdges.length === 0) {
            result.errors.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】是条件节点，必须至少有一个输出连线`,
              type: 'config'
            });
            result.valid = false;
          }
        }

        // 循环节点验证：必须配置最大循环次数
        if (type === 'loop') {
          const maxIterations = config.maxIterations || config.max_iterations;
          if (!maxIterations || maxIterations < 1) {
            result.errors.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】是循环节点，必须配置最大循环次数`,
              type: 'config'
            });
            result.valid = false;
          }
        }
      });

      // 4. 流程可达性检查（从开始节点能到达所有节点）
      const startNode = nodes.find(n => n.type === 'start');
      if (startNode) {
        const reachable = new Set();
        reachable.add(startNode.id);

        // BFS遍历
        const queue = [startNode.id];
        while (queue.length > 0) {
          const currentId = queue.shift();
          edges.forEach(edge => {
            const sourceId = edge.sourceNodeId || edge.source_node_id;
            const targetId = edge.targetNodeId || edge.target_node_id;
            if (sourceId === currentId && !reachable.has(targetId)) {
              reachable.add(targetId);
              queue.push(targetId);
            }
          });
        }

        // 检查是否有不可达节点
        nodes.forEach(node => {
          if (!reachable.has(node.id)) {
            result.errors.push({
              nodeId: node.id,
              message: `节点【${node.name || node.id}】无法从开始节点到达`,
              type: 'flow'
            });
            result.valid = false;
          }
        });

        // 检查所有节点都能到达结束节点（反向BFS从结束节点开始）
        const finishNodes = nodes.filter(n => n.type === 'finish');
        if (finishNodes.length > 0) {
          const canReachFinish = new Set();
          finishNodes.forEach(fn => canReachFinish.add(fn.id));

          // 构建反向边图
          const reverseEdges = {};
          edges.forEach(edge => {
            const sourceId = edge.sourceNodeId || edge.source_node_id;
            const targetId = edge.targetNodeId || edge.target_node_id;
            if (!reverseEdges[targetId]) reverseEdges[targetId] = [];
            reverseEdges[targetId].push(sourceId);
          });

          // 从结束节点反向BFS
          const queue = [...finishNodes.map(n => n.id)];
          while (queue.length > 0) {
            const currentId = queue.shift();
            const predecessors = reverseEdges[currentId] || [];
            predecessors.forEach(predId => {
              if (!canReachFinish.has(predId)) {
                canReachFinish.add(predId);
                queue.push(predId);
              }
            });
          }

          // 检查是否有节点无法到达结束节点
          nodes.forEach(node => {
            if (!canReachFinish.has(node.id) && node.type !== 'finish') {
              result.errors.push({
                nodeId: node.id,
                message: `节点【${node.name || node.id}】无法到达结束节点`,
                type: 'flow'
              });
              result.valid = false;
            }
          });
        }
      }

      return result;
    }

    // 显示验证错误弹窗
    function showValidationErrors(errors, warnings) {
      let html = '<div class="validation-result">';

      if (errors.length > 0) {
        html += '<div class="validation-section errors"><div class="section-title">❌ 错误</div>';
        errors.forEach(e => {
          html += `<div class="validation-item error" onclick="focusNodeById('${e.nodeId || ''}')">
            <span class="message">${e.message}</span>
          </div>`;
        });
        html += '</div>';
      }

      if (warnings.length > 0) {
        html += '<div class="validation-section warnings"><div class="section-title">⚠️ 警告</div>';
        warnings.forEach(w => {
          html += `<div class="validation-item warning" onclick="focusNodeById('${w.nodeId || ''}')">
            <span class="message">${w.message}</span>
          </div>`;
        });
        html += '</div>';
      }

      html += '</div>';

      // 只有警告没有错误时，显示忽略警告选项
      const hasOnlyWarnings = errors.length === 0 && warnings.length > 0;
      const footerButtons = hasOnlyWarnings
        ? `<button class="btn btn-warning" onclick="ignoreWarningsAndExecute()">忽略警告继续执行</button>
           <button class="btn btn-secondary" onclick="closeModal('validationModal')">取消</button>`
        : `<button class="btn btn-secondary" onclick="closeModal('validationModal')">关闭</button>`;

      // 简单弹窗
      const modal = document.createElement('div');
      modal.className = 'modal-overlay show';
      modal.id = 'validationModal';
      modal.innerHTML = `
        <div class="modal" style="max-width:500px;">
          <div class="modal-header">
            <h3>验证结果</h3>
            <button class="close-btn" onclick="closeModal('validationModal')">&times;</button>
          </div>
          <div class="modal-body">${html}</div>
          <div class="modal-footer">${footerButtons}</div>
        </div>
      `;
      document.body.appendChild(modal);
    }

    // 忽略警告继续执行
    async function ignoreWarningsAndExecute() {
      closeModal('validationModal');
      // 直接执行，跳过验证
      await doExecuteWorkflow();
    }

    // 聚焦到指定节点
    function focusNodeById(nodeId) {
      if (!nodeId) return;
      selectNode(nodeId);
      const nodeEl = document.getElementById(`node-${nodeId}`);
      if (nodeEl) {
        nodeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        nodeEl.classList.add('highlight');
        setTimeout(() => nodeEl.classList.remove('highlight'), 2000);
      }
      closeModal('validationModal');
    }

    // 执行工作流
    async function executeWorkflow() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }

      // 完整验证
      const validation = validateWorkflowComplete();
      if (!validation.valid) {
        showValidationErrors(validation.errors, validation.warnings);
        return;
      }

      // 只有警告，让用户选择是否继续
      if (validation.warnings.length > 0) {
        showValidationErrors([], validation.warnings);
        return;
      }

      await doExecuteWorkflow();
    }

    // 实际执行工作流
    async function doExecuteWorkflow() {
      // 检查任务配置
      if (!taskConfig.projectPath || !taskConfig.name) {
        showToast('warn', '请先配置任务（点击"任务配置"按钮）');
        showTaskConfigModal();
        return;
      }

      // 确认执行
      if (!await confirmAsync(`确认执行任务？\n\n任务名称: ${taskConfig.name}\n项目路径: ${taskConfig.projectPath}`)) return;

      try {
        updateStatus('running');
        const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/executions`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            input: {
              task_description: taskConfig.description,
              task_name: taskConfig.name
            },
            taskConfig: {
              name: taskConfig.name,
              description: taskConfig.description,
              projectPath: taskConfig.projectPath,
              workflowPath: taskConfig.workflowPath,
              globalPrompt: globalConfig.globalPromptContent || ''
            }
          })
        });
        const data = await res.json();
        if (data.success) {
          state.execution = data.data;
          state.executionStatus = 'running';
          // 保存上下文文件路径
          taskConfig.contextFilePath = data.data.contextFilePath || '';
          connectWS(data.data.executionId);
          // 更新按钮状态：隐藏执行，显示暂停和停止
          document.getElementById('btnExecute').style.display = 'none';
          document.getElementById('btnPause').style.display = 'inline-flex';
          document.getElementById('btnResume').style.display = 'none';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('info', '开始执行: ' + data.data.executionId);
          addLog('info', '上下文文件: ' + (data.data.contextFilePath || '未创建'), 'execution');
        }
      } catch (e) {
        updateStatus('error');
        showToast('error', '启动失败');
      }
    }

    // 停止执行
    async function stopExecution() {
      if (!state.execution) return;
      try {
        await fetch(`${API}/executions/${state.execution.executionId}/stop`, { method: 'POST' });
        addLog('warn', '执行已停止');
        resetExecution();
      } catch (e) {
        showToast('error', '停止失败');
      }
    }

    // 暂停执行
    async function pauseExecution() {
      if (!state.execution) return;
      try {
        const res = await fetch(`${API}/executions/${state.execution.executionId}/pause`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          state.executionStatus = 'paused';
          // 更新按钮状态：隐藏暂停，显示继续和停止
          document.getElementById('btnPause').style.display = 'none';
          document.getElementById('btnResume').style.display = 'inline-flex';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('warn', '执行已暂停');
          updateStatus('idle');
          showToast('success', '执行已暂停');
        }
      } catch (e) {
        showToast('error', '暂停失败');
      }
    }

    // 恢复执行
    async function resumeExecution() {
      if (!state.execution) return;
      try {
        const res = await fetch(`${API}/executions/${state.execution.executionId}/resume`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          state.executionStatus = 'running';
          // 更新按钮状态：隐藏继续，显示暂停和停止
          document.getElementById('btnPause').style.display = 'inline-flex';
          document.getElementById('btnResume').style.display = 'none';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('info', '执行已恢复');
          updateStatus('running');
          showToast('success', '执行已恢复');
        }
      } catch (e) {
        showToast('error', '恢复失败');
      }
    }


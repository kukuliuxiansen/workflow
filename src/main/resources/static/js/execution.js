// 工作流执行

    function resetExecution() {
      document.getElementById('btnExecute').style.display = 'inline-flex';
      document.getElementById('btnStop').style.display = 'none';
      updateStatus('idle');
      state.execution = null;
      state.nodeStatus.clear();
    }

    // WebSocket
    function connectWS(executionId) {
      const ws = new WebSocket(`ws://${location.host}/ws/executions?executionId=${executionId}`);
      ws.onmessage = (e) => {
        const msg = JSON.parse(e.data);
        if (msg.type === 'log') {
          const logType = msg.data.logType || 'execution';
          addLog(msg.data.level, msg.data.message, logType);
        }
        else if (msg.type === 'agentMessage') {
          addAgentMessage(msg.data.agentId, msg.data.message, msg.data.messageType);
        }
        else if (msg.type === 'agentInteraction') {
          addAgentLog(msg.data.fromAgent, msg.data.toAgent, msg.data.action, msg.data.detail);
        }
        else if (msg.type === 'status') {
          if (msg.data.status === 'completed') {
            updateStatus('success');
            addLog('success', '执行完成');
            resetExecution();
          } else if (msg.data.status === 'failed') {
            updateStatus('error');
            addLog('error', '执行失败: ' + (msg.data.error || ''));
            resetExecution();
          }
        } else if (msg.type === 'nodeStatus') {
          state.nodeStatus.set(msg.data.nodeId, msg.data.status);
          renderCanvas();
        }
      };
      ws.onclose = () => addLog('info', '连接关闭');
      state.ws = ws;
    }

    // 验证工作流
    async function validateWorkflow() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }
      try {
        const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/validate`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          const r = data.data;
          if (r.valid) {
            showToast('success', '验证通过');
            addLog('success', `验证通过 (${r.nodeCount}节点, ${r.edgeCount}连线)`);
          } else {
            showToast('error', `验证失败: ${r.errors.length}个错误`);
            r.errors.forEach(e => addLog('error', e.message));
          }
          r.warnings.forEach(w => addLog('warn', w.message));
        }
      } catch (e) {
        showToast('error', '验证失败');
      }
    }

    // 导出YAML
    async function exportYAML() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }
      const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/export`);
      const yaml = await res.text();
      const blob = new Blob([yaml], { type: 'text/yaml' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `${state.currentWorkflow.id}.yaml`;
      a.click();
      showToast('success', '导出成功');
    }

    // 历史记录状态
    state.historyRecords = [];
    state.selectedHistoryId = null;
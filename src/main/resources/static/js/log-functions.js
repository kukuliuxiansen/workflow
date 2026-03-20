// 日志相关函数

    // Agent间交互日志
    function addAgentLog(fromAgent, toAgent, action, detail) {
      const msg = `<span class="agent-from">[${fromAgent}]</span> → <span class="agent-to">[${toAgent}]</span> ${action}: ${detail}`;
      addLog('info', msg, 'agent');
    }

    // Agent消息日志
    function addAgentMessage(agentId, message, messageType = 'response') {
      const icon = messageType === 'response' ? '📤' : messageType === 'request' ? '📥' : '💬';
      const msg = `<span class="agent-name">${icon} ${agentId}</span>: ${message}`;
      addLog('info', msg, 'agent');
    }

    // 清空当前日志
    function clearCurrentLogs() {
      const tab = state.currentLogTab;
      if (tab === 'operation') {
        fetch(`${API}/logs/recent`, { method: 'DELETE' })
          .then(() => {
            state.operationLogs = [];
            renderOperationLogs([]);
            updateLogCounts();
          });
      } else {
        state.logs[tab] = [];
        const contentId = tab === 'agent' ? 'agentLogContent' : 'execLogContent';
        document.getElementById(contentId).innerHTML = '';
        updateLogCounts();
      }
    }

    // 导出当前日志
    function exportCurrentLogs() {
      const tab = state.currentLogTab;
      const logs = state.logs[tab];
      const text = logs.map(l => `[${l.time}] [${l.level.toUpperCase()}] ${l.msg}`).join('\n');
      const blob = new Blob([text], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${tab === 'agent' ? 'agent' : 'execution'}_log_${new Date().toISOString().slice(0,10)}.txt`;
      a.click();
      URL.revokeObjectURL(url);
    }

    // 切换日志标签页
    function switchLogTab(tab) {
      state.currentLogTab = tab;
      document.querySelectorAll('.log-main-tab').forEach(t => t.classList.remove('active'));
      event.target.closest('.log-main-tab').classList.add('active');

      document.querySelectorAll('.log-content').forEach(c => c.classList.remove('active'));
      let contentId = 'execLogContent';
      if (tab === 'agent') contentId = 'agentLogContent';
      else if (tab === 'api') contentId = 'apiLogContent';
      else if (tab === 'operation') contentId = 'operationLogContent';
      document.getElementById(contentId).classList.add('active');

      if (tab === 'operation') {
        refreshOperationLogs();
      } else if (tab === 'api') {
        renderApiLogs();
      }
    }

    // 更新日志计数
    function updateLogCounts() {
      const execLogCount = document.getElementById('execLogCount');
      const agentLogCount = document.getElementById('agentLogCount');
      const apiLogCount = document.getElementById('apiLogCount');
      const opLogCount = document.getElementById('opLogCount');
      if (execLogCount) execLogCount.textContent = state.logs.execution.length;
      if (agentLogCount) agentLogCount.textContent = state.logs.agent.length;
      if (apiLogCount) apiLogCount.textContent = state.logs.api.length;
      if (opLogCount) opLogCount.textContent = state.operationLogs.length;
    }
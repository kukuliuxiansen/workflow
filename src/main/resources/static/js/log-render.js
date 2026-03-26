// 日志渲染函数

    // 渲染所有日志
    function renderLogs() {
      // 确保 state.logs 存在
      if (!state.logs) {
        state.logs = { execution: [], agent: [], api: [] };
      }

      // 渲染执行日志
      const execContent = document.getElementById('execLogContent');
      if (execContent && state.logs.execution) {
        execContent.innerHTML = state.logs.execution.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${msg}</span></div>`;
        }).join('');
        execContent.scrollTop = execContent.scrollHeight;
      }

      // 渲染Agent日志
      const agentContent = document.getElementById('agentLogContent');
      if (agentContent && state.logs.agent) {
        agentContent.innerHTML = state.logs.agent.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          const agentId = l.agentId || '';
          const agentPrefix = agentId ? `<span class="agent-name">🤖 ${agentId}</span>: ` : '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${agentPrefix}${msg}</span></div>`;
        }).join('');
        agentContent.scrollTop = agentContent.scrollHeight;
      }

      updateLogCounts();
    }

    // 渲染API日志
    function renderApiLogs() {
      const container = document.getElementById('apiLogContent');
      if (!container) return;

      if (!state.logs || !state.logs.api || state.logs.api.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#888;">暂无API日志</div>';
        return;
      }

      const html = state.logs.api.map(l => {
        const statusColor = l.success ? '#4CAF50' : '#f44336';
        const methodColor = l.method === 'GET' ? '#2196F3' : l.method === 'POST' ? '#4CAF50' : l.method === 'DELETE' ? '#f44336' : '#FF9800';
        return `
          <div style="border:1px solid #3a3a3a;border-radius:4px;margin-bottom:8px;background:#1a1a1a;">
            <div style="padding:8px;display:flex;gap:8px;align-items:center;border-bottom:1px solid #3a3a3a;">
              <span style="color:#22d3ee;font-size:10px;cursor:pointer;" onclick="navigator.clipboard.writeText('${l.traceId}')">${l.traceId}</span>
              <span style="color:${methodColor};font-weight:bold;min-width:50px;">${l.method}</span>
              <span style="color:#fff;flex:1;">${l.url}</span>
              <span style="color:${statusColor};">${l.status}</span>
              <span style="color:#888;">${l.duration}</span>
            </div>
            <div style="padding:8px;font-size:11px;">
              <div style="color:#4CAF50;margin-bottom:4px;">Request:</div>
              <pre style="margin:0;white-space:pre-wrap;word-break:break-all;color:#aaa;max-height:100px;overflow:auto;">${escapeHtml(l.request || '(empty)')}</pre>
            </div>
            <div style="padding:8px;font-size:11px;border-top:1px solid #3a3a3a;">
              <div style="color:#2196F3;margin-bottom:4px;">Response:</div>
              <pre style="margin:0;white-space:pre-wrap;word-break:break-all;color:#aaa;max-height:100px;overflow:auto;">${escapeHtml(l.response || '(empty)')}</pre>
            </div>
          </div>`;
      }).join('');

      container.innerHTML = html;
    }

    // 渲染操作日志
    function renderOperationLogs(logs) {
      const container = document.getElementById('opLogList');
      if (!container) return;

      if (!logs || logs.length === 0) {
        container.innerHTML = '<div class="empty-state" style="text-align:center;padding:20px;color:#888;">暂无日志</div>';
        return;
      }

      const typeColors = {
        'NODE': '#4CAF50', 'AI': '#2196F3', 'API': '#FF9800',
        'WORKFLOW': '#9C27B0', 'ERROR': '#f44336',
        'AI_INPUT': '#2196F3', 'AI_OUTPUT': '#00BCD4'
      };

      container.innerHTML = logs.map(log => {
        const typeColor = typeColors[log.type] || '#888';
        const successClass = log.success === false ? 'op-log-fail' : '';

        return `
          <div class="op-log-item ${successClass}" style="padding:8px;border-bottom:1px solid #333;font-size:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
              <span style="background:${typeColor};color:#fff;padding:2px 6px;border-radius:3px;font-size:11px;">${log.type || '-'}</span>
              <span style="color:#888;">${log.timestamp || '-'}</span>
            </div>
            <div style="color:#ccc;margin:4px 0;">${log.operation || '-'}</div>
            ${log.executionId ? `<div style="color:#888;font-size:11px;">执行ID: ${log.executionId}</div>` : ''}
            ${log.nodeId ? `<div style="color:#888;font-size:11px;">节点: ${log.nodeId}</div>` : ''}
            ${log.input ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#4CAF50;">输入</summary><pre style="background:#1a1a1a;padding:8px;margin:4px 0;overflow-x:auto;white-space:pre-wrap;word-break:break-all;">${escapeHtml(typeof log.input === 'object' ? JSON.stringify(log.input, null, 2) : log.input)}</pre></details>` : ''}
            ${log.output ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#2196F3;">输出</summary><pre style="background:#1a1a1a;padding:8px;margin:4px 0;overflow-x:auto;white-space:pre-wrap;word-break:break-all;">${escapeHtml(typeof log.output === 'object' ? JSON.stringify(log.output, null, 2) : log.output)}</pre></details>` : ''}
            ${log.error ? `<div style="color:#f44336;margin-top:4px;">错误: ${escapeHtml(log.error)}</div>` : ''}
          </div>`;
      }).join('');
    }

    // 刷新操作日志
    async function refreshOperationLogs() {
      const typeFilter = document.getElementById('opLogTypeFilter')?.value || '';
      const dateFilter = document.getElementById('opLogDateFilter')?.value || '';

      try {
        let url = `${API}/logs/file?limit=200`;
        if (dateFilter) url += `&date=${dateFilter}`;

        const res = await fetch(url);
        const data = await res.json();

        if (data.success) {
          let logs = data.data || [];
          if (typeFilter) logs = logs.filter(l => l.type === typeFilter);
          state.operationLogs = logs;
          renderOperationLogs(logs);
          updateLogCounts();
        }
      } catch (e) {
        console.error('获取操作日志失败:', e);
      }
    }
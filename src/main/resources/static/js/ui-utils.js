// UI辅助函数

    let resizing = null;

    // 复制TraceID
    function copyTraceId(traceId) {
      navigator.clipboard.writeText(traceId).then(() => {
        showToast('success', 'TraceID已复制: ' + traceId);
      }).catch(() => {
        showToast('error', '复制失败');
      });
    }

    // 生成TraceID
    function generateTraceId() {
      return 'TRC-' + Date.now() + '-' + Math.random().toString(16).substr(2, 4).toUpperCase();
    }

    // 保存当前任务配置
    async function saveCurrentTaskConfig() {
      if (!taskConfig.executionId) {
        showToast('warn', '请先选择一条执行记录');
        return;
      }

      try {
        const res = await fetch(`${API}/executions/${taskConfig.executionId}/task-config`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: taskConfig.name,
            description: taskConfig.description,
            projectPath: taskConfig.projectPath,
            workflowId: state.currentWorkflow?.id
          })
        });
        const data = await res.json();
        if (data.success) {
          showToast('success', '任务配置已保存');
        } else {
          showToast('error', data.message || '保存失败');
        }
      } catch (e) {
        showToast('error', '保存失败');
      }
    }

    // 面板大小调整
    function onResize(e) {
      if (!resizing) return;
      const panel = document.getElementById(resizing.panelId);

      if (resizing.panelId === 'sidebar') {
        const newWidth = Math.max(200, Math.min(400, e.clientX));
        panel.style.width = newWidth + 'px';
      } else if (resizing.panelId === 'rightPanel') {
        const newWidth = Math.max(250, Math.min(500, window.innerWidth - e.clientX));
        panel.style.width = newWidth + 'px';
      } else if (resizing.panelId === 'logPanel') {
        const newHeight = Math.max(100, Math.min(400, window.innerHeight - e.clientY));
        panel.style.height = newHeight + 'px';
      }
    }

    // 停止调整大小
    function stopResize() {
      if (resizing) {
        const panel = document.getElementById(resizing.panelId);
        const prefs = getPrefs();
        if (resizing.panelId === 'sidebar') {
          prefs.sidebarWidth = panel.offsetWidth;
        } else if (resizing.panelId === 'rightPanel') {
          prefs.rightPanelWidth = panel.offsetWidth;
        } else if (resizing.panelId === 'logPanel') {
          prefs.logPanelHeight = panel.offsetHeight;
        }
        savePrefs(prefs);
      }
      resizing = null;
      document.removeEventListener('mousemove', onResize);
      document.removeEventListener('mouseup', stopResize);
    }

    // 选择节点类型
    function selectNodeType(el) {
      document.querySelectorAll('.node-type-card').forEach(c => c.classList.remove('selected'));
      el.classList.add('selected');
      state.selectedNodeType = el.dataset.type;
    }

    // 显示节点右键菜单
    function showNodeContextMenu(e, nodeId) {
      e.preventDefault();
      state.contextMenuNode = nodeId;
      const menu = document.getElementById('contextMenu');
      menu.style.left = e.clientX + 'px';
      menu.style.top = e.clientY + 'px';
      menu.classList.add('show');
    }

    function showEdgeContextMenu(event, edgeId) {
      event.preventDefault();
      currentEdgeId = edgeId;
      const menu = document.getElementById('edgeContextMenu');
      menu.style.left = event.clientX + 'px';
      menu.style.top = event.clientY + 'px';
      menu.classList.add('show');
    }

    function edgeContextMenuAction(action) {
      closeContextMenu();

      if (!currentEdgeId) return;

      switch(action) {
        case 'edit-label':
          const label = prompt('输入连线标签:');
          if (label !== null) {
            updateEdgeLabel(currentEdgeId, label);
          }
          break;
        case 'toggle-type-success':
          updateEdgeType(currentEdgeId, 'success');
          break;
        case 'toggle-type-failure':
          updateEdgeType(currentEdgeId, 'failure');
          break;
        case 'toggle-type-default':
          updateEdgeType(currentEdgeId, 'default');
          break;
        case 'delete':
          deleteEdgeById(currentEdgeId);
          break;
      }

      currentEdgeId = null;
    }

    async function updateEdgeLabel(edgeId, label) {
      if (!state.currentWorkflow || !state.currentWorkflow.edges) return;

      const edge = state.currentWorkflow.edges.find(e =>
        `${(e.sourceNodeId || e.source_node_id)}-${(e.targetNodeId || e.target_node_id)}` === edgeId || e.id === edgeId
      );
      if (edge) {
        // TODO: 调用后端API更新连线标签
        edge.label = label;
        await selectWorkflow(state.currentWorkflow.id);
      }
    }

    function updateEdgeType(edgeId, type) {
      if (!state.currentWorkflow || !state.currentWorkflow.edges) return;

      const edge = state.currentWorkflow.edges.find(e =>
        `${(e.sourceNodeId || e.source_node_id)}-${(e.targetNodeId || e.target_node_id)}` === edgeId || e.id === edgeId
      );
      if (edge) {
        // 更新需要调用后端API
        // TODO: 实现更新边的API
        edge.edge_type = type;
        renderCanvas();
      }
    }

    // 导出功能
    function exportCanvasAsImage() {
      showToast('info', '正在导出图片...');

      // 使用html2canvas或svg转图片
      const svg = document.getElementById('edgesSvg');
      const content = document.getElementById('canvasContent');

      // 简单实现：创建截图
      const canvas = document.createElement('canvas');
      const rect = content.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;

      // 这里需要更复杂的实现来捕获SVG和HTML节点
      // 暂时提示用户
      showToast('warn', '图片导出功能开发中，请使用浏览器截图');
    }

    function exportWorkflowAsJson() {
      if (!state.currentWorkflow) return;

      const dataStr = JSON.stringify(state.currentWorkflow, null, 2);
      const blob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = `${state.currentWorkflow.name || 'workflow'}.json`;
      a.click();

      URL.revokeObjectURL(url);
      showToast('success', '工作流已导出');
    }

    // 关闭所有右键菜单
    function closeContextMenu() {
      document.querySelectorAll('.context-menu').forEach(menu => {
        menu.classList.remove('show');
      });
      state.contextMenuNode = null;
    }

    // 点击其他地方关闭菜单
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.context-menu')) {
        closeContextMenu();
      }
    });

    function showToast(type, msg) {
      const container = document.getElementById('toastContainer');
      const toast = document.createElement('div');
      toast.className = `toast ${type}`;
      toast.innerHTML = `<span style="margin-right:8px;">${type === 'success' ? '✓' : type === 'error' ? '✕' : type === 'warn' ? '⚠' : 'ℹ'}</span>${msg}`;
      container.appendChild(toast);
      setTimeout(() => toast.remove(), 3000);
    }

    // 确认对话框（替代confirm）
    function showConfirm(message, onConfirm, onCancel) {
      const modalHtml = `
        <div class="modal-overlay show" id="confirmModal" onclick="if(event.target===this){this.remove();if(onCancel)onCancel();}">
          <div class="modal" style="background:linear-gradient(145deg, #2d2d2d, #252525);border:1px solid #444;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,0.5),0 0 0 1px rgba(255,255,255,0.05);min-width:360px;overflow:hidden;" onclick="event.stopPropagation()">
            <div style="padding:32px;text-align:center;">
              <div style="width:64px;height:64px;margin:0 auto 20px;background:linear-gradient(135deg, #fbbf24, #f59e0b);border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 20px rgba(251,191,36,0.3);">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 9v4M12 17h.01"/>
                  <circle cx="12" cy="12" r="10"/>
                </svg>
              </div>
              <div style="font-size:15px;color:#e0e0e0;white-space:pre-wrap;line-height:1.6;">${message}</div>
            </div>
            <div style="padding:0 24px 24px;display:flex;gap:12px;justify-content:center;">
              <button onclick="this.closest('.modal-overlay').remove();if(onCancel)onCancel();" style="flex:1;padding:12px 24px;background:#333;border:1px solid #444;border-radius:10px;color:#aaa;font-size:14px;cursor:pointer;transition:all 0.2s;" onmouseover="this.style.background='#3a3a3a';this.style.color='#fff';" onmouseout="this.style.background='#333';this.style.color='#aaa';">取消</button>
              <button id="confirmBtn" style="flex:1;padding:12px 24px;background:linear-gradient(135deg, #6366f1, #4f46e5);border:none;border-radius:10px;color:#fff;font-size:14px;font-weight:500;cursor:pointer;transition:all 0.2s;box-shadow:0 4px 15px rgba(99,102,241,0.3);" onmouseover="this.style.transform='translateY(-1px)';this.style.boxShadow='0 6px 20px rgba(99,102,241,0.4)';" onmouseout="this.style.transform='translateY(0)';this.style.boxShadow='0 4px 15px rgba(99,102,241,0.3)';">确定</button>
            </div>
          </div>
        </div>
      `;
      document.body.insertAdjacentHTML('beforeend', modalHtml);
      document.getElementById('confirmBtn').onclick = function() {
        document.getElementById('confirmModal').remove();
        if (onConfirm) onConfirm();
      };
    }

    // 异步确认（返回Promise）

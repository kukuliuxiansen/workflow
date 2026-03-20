// 搜索功能
    function toggleSearchPanel() {
      const panel = document.getElementById('searchPanel');
      if (panel.classList.contains('show')) {
        closeSearchPanel();
      } else {
        panel.classList.add('show');
        document.getElementById('nodeSearchInput').focus();
      }
    }

    function closeSearchPanel() {
      const panel = document.getElementById('searchPanel');
      if (panel) panel.classList.remove('show');
    }

    function handleNodeSearch(query) {
      const resultsEl = document.getElementById('searchResults');
      if (!query.trim()) {
        resultsEl.innerHTML = '';
        return;
      }

      const lowerQuery = query.toLowerCase();
      const results = [];

      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        state.currentWorkflow.nodes.forEach(node => {
          if (node.name.toLowerCase().includes(lowerQuery) ||
              (node.description && node.description.toLowerCase().includes(lowerQuery))) {
            results.push(node);
          }
        });
      }

      if (results.length === 0) {
        resultsEl.innerHTML = '<div style="padding:10px;text-align:center;color:#888;">无匹配结果</div>';
        return;
      }

      resultsEl.innerHTML = results.map(node => {
        const icon = getNodeIcon(node.type);
        return `<div class="search-result-item" onclick="focusNode('${node.id}')">
          <span>${icon}</span>
          <span>${node.name}</span>
          <span style="font-size:11px;color:#888;margin-left:auto;">${node.type}</span>
        </div>`;
      }).join('');
    }

    function focusNode(nodeId) {
      const node = state.currentWorkflow?.nodes?.find(n => n.id === nodeId);
      if (!node) return;

      // 平移画布使节点居中
      const canvasEl = document.getElementById('canvas');
      state.panX = canvasEl.clientWidth / 2 - node.position_x * state.zoom - 90 * state.zoom;
      state.panY = canvasEl.clientHeight / 2 - node.position_y * state.zoom - 40 * state.zoom;

      // 选中节点
      clearNodeSelection();
      state.selectedNode = node;
      state.selectedNodes.add(nodeId);

      updateCanvasTransform();
      renderCanvas();
      renderPropertyPanel();
      closeSearchPanel();
      updateSelectedCount();
    }

    function getNodeIcon(type) {
      const icons = {
        'start': '▶️',
        'finish': '⏹️',
        'agent_execution': '🤖',
        'api_call': '🌐',
        'condition': '❓',
        'parallel': '⚡',
        'loop': '🔄',
        'wait': '⏸️',
        'human_review': '👤',
        'subworkflow': '📦'
      };
      return icons[type] || '📄';
    }

    // 快捷键帮助
    function toggleShortcutHelp() {
      const panel = document.getElementById('shortcutHelpPanel');
      if (panel.classList.contains('show')) {
        closeShortcutHelp();
      } else {
        panel.classList.add('show');
      }
    }

    function closeShortcutHelp() {
      const panel = document.getElementById('shortcutHelpPanel');
      if (panel) panel.classList.remove('show');
    }
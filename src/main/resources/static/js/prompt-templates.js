
// 加载提示词模板列表
async function loadPromptTemplates() {
  const container = document.getElementById('promptsNodeTypesList');
  container.innerHTML = '<div class="empty-state-mini">加载中...</div>';

  try {
    const res = await fetch(`${API}/prompts/node-types`);
    const data = await res.json();

    if (!data.success) {
      container.innerHTML = '<div class="empty-state-mini">加载失败</div>';
      return;
    }

    const types = data.data;
    if (!types || types.length === 0) {
      container.innerHTML = '<div class="empty-state-mini">暂无节点类型</div>';
      return;
    }

    let html = '<div class="prompt-types-list">';
    for (const type of types) {
      const statusBadge = type.hasCustom
        ? '<span style="color:#ffc107;font-size:10px;">已自定义</span>'
        : (type.hasDefault ? '<span style="color:#4caf50;font-size:10px;">默认</span>' : '');
      html += `
        <div class="prompt-type-item" style="padding:10px;border:1px solid #3a3a3a;border-radius:6px;margin-bottom:8px;cursor:pointer;background:#2a2a2a;"
             onclick="showPromptTemplateDetail('${type.type}')">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span style="font-weight:500;">${type.name}</span>
            ${statusBadge}
          </div>
          <div style="font-size:11px;color:#888;margin-top:4px;">类型: ${type.type}</div>
        </div>
      `;
    }
    html += '</div>';
    container.innerHTML = html;

  } catch (e) {
    container.innerHTML = '<div class="empty-state-mini">加载失败: ' + e.message + '</div>';
  }
}

// 显示提示词模板详情弹窗
async function showPromptTemplateDetail(nodeType) {
  try {
    const res = await fetch(`${API}/prompts/${nodeType}`);
    const data = await res.json();

    if (!data.success) {
      showToast('error', '加载模板失败');
      return;
    }

    const info = data.data;
    const currentTemplate = info.currentTemplate || '';

    const modalHtml = `
      <div class="modal-overlay" onclick="closePromptModal(event)" id="promptModalOverlay">
        <div class="modal-content" style="max-width:800px;max-height:90vh;overflow:hidden;display:flex;flex-direction:column;" onclick="event.stopPropagation()">
          <div class="modal-header" style="display:flex;justify-content:space-between;align-items:center;padding:16px;border-bottom:1px solid #3a3a3a;">
            <h3 style="margin:0;font-size:16px;">${info.name} - 提示词模板</h3>
            <button onclick="closePromptModal()" style="background:none;border:none;color:#888;font-size:20px;cursor:pointer;">&times;</button>
          </div>
          <div class="modal-body" style="flex:1;overflow-y:auto;padding:16px;">
            <div style="margin-bottom:16px;">
              <div style="font-size:12px;color:#888;margin-bottom:8px;">
                <strong>可用变量:</strong>
                ${info.variables ? info.variables.map(v => `<code style="background:#333;padding:2px 6px;border-radius:3px;margin:2px;display:inline-block;">{${v.name}}</code>`).join('') : '无'}
              </div>
            </div>
            <div style="margin-bottom:12px;">
              <label style="font-size:13px;color:#888;display:block;margin-bottom:6px;">模板内容:</label>
              <textarea id="promptTemplateContent" style="width:100%;min-height:300px;background:#1a1a1a;color:#e0e0e0;border:1px solid #3a3a3a;border-radius:6px;padding:12px;font-family:Monaco,monospace;font-size:12px;resize:vertical;">${escapeHtml(currentTemplate)}</textarea>
            </div>
            ${info.customTemplate ? `
              <div style="margin-bottom:12px;">
                <label style="font-size:13px;color:#888;display:block;margin-bottom:6px;">默认模板 (参考):</label>
                <pre style="background:#1a1a1a;color:#888;border:1px solid #3a3a3a;border-radius:6px;padding:12px;font-size:11px;max-height:150px;overflow:auto;white-space:pre-wrap;">${escapeHtml(info.defaultTemplate || '')}</pre>
              </div>
            ` : ''}
          </div>
          <div class="modal-footer" style="padding:12px 16px;border-top:1px solid #3a3a3a;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="resetPromptTemplate('${nodeType}')">重置为默认</button>
            <button class="btn btn-primary" onclick="savePromptTemplate('${nodeType}')">保存</button>
          </div>
        </div>
      </div>
    `;

    // 移除已存在的弹窗
    const existing = document.getElementById('promptModalOverlay');
    if (existing) existing.remove();

    document.body.insertAdjacentHTML('beforeend', modalHtml);

  } catch (e) {
    showToast('error', '加载失败: ' + e.message);
  }
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function closePromptModal(event) {
  if (event && event.target !== event.currentTarget) return;
  const modal = document.getElementById('promptModalOverlay');
  if (modal) modal.remove();
}

// 保存提示词模板
async function savePromptTemplate(nodeType) {
  const content = document.getElementById('promptTemplateContent').value;

  try {
    const res = await fetch(`${API}/prompts/${nodeType}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ template: content })
    });
    const data = await res.json();

    if (data.success) {
      showToast('success', '提示词模板已保存');
      closePromptModal();
      loadPromptTemplates();
    } else {
      showToast('error', data.message || '保存失败');
    }
  } catch (e) {
    showToast('error', '保存失败: ' + e.message);
  }
}

// 重置提示词模板
async function resetPromptTemplate(nodeType) {
  if (!await confirmAsync('确定要重置为默认模板吗？\n这将清除您的自定义设置。')) {
    return;
  }

  try {
    const res = await fetch(`${API}/prompts/${nodeType}`, {
      method: 'DELETE'
    });
    const data = await res.json();

    if (data.success) {
      showToast('success', '已重置为默认模板');
      closePromptModal();
      loadPromptTemplates();
    } else {
      showToast('error', data.message || '重置失败');
    }
  } catch (e) {
    showToast('error', '重置失败: ' + e.message);
  }
}


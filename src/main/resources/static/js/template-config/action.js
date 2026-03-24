/**
 * 模板配置 - 操作模块
 */
const TemplateAction = {
    async init() {
        await this.loadTemplates();
        await this.loadAllVariables();
    },

    async loadTemplates() {
        TemplateState.templates = await TemplateApi.list();
        this.renderList();
        const def = TemplateState.templates.find(t => t.isDefault) || TemplateState.templates[0];
        if (def) await this.select(def.id);
    },

    async loadAllVariables() {
        for (const type of ['decision', 'review', 'retry']) {
            TemplateState.variables[type] = await TemplateApi.getVariables(type);
        }
        this.renderVariables();
    },

    renderList() {
        const container = document.getElementById('templateList');
        const { templates, currentTemplate } = TemplateState;
        if (!templates.length) {
            container.innerHTML = '<div class="empty-state">暂无模板</div>';
            return;
        }
        container.innerHTML = templates.map(t => `
            <div class="template-item ${currentTemplate?.id === t.id ? 'active' : ''}" onclick="TemplateAction.select('${t.id}')">
                <div class="name">${t.name}${t.isDefault ? '<span class="badge">默认</span>' : ''}</div>
                <div class="meta">${t.id}</div>
                <div class="actions">
                    ${!t.isDefault ? `<button class="action-btn" onclick="event.stopPropagation();TemplateAction.setDefault('${t.id}')">默认</button>` : ''}
                    <button class="action-btn" onclick="event.stopPropagation();TemplateAction.copy('${t.id}')">复制</button>
                    ${!t.isDefault ? `<button class="action-btn danger" onclick="event.stopPropagation();TemplateAction.delete('${t.id}')">删除</button>` : ''}
                </div>
            </div>
        `).join('');
    },

    async setDefault(id) {
        console.log('[setDefault] 设置默认模板:', id);
        const result = await TemplateApi.setDefault(id);
        console.log('[setDefault] API返回:', result);
        if (result.success) {
            await this.loadTemplates();
        } else {
            await showAlertDialog('设置默认失败: ' + (result.message || '未知错误'));
        }
    },

    async copy(id) {
        console.log('[copy] 复制模板:', id);
        const result = await TemplateApi.copy(id);
        console.log('[copy] API返回:', result);
        if (result.success) {
            await this.loadTemplates();
            await this.select(result.data.id);
        } else {
            await showAlertDialog('复制失败: ' + (result.message || '未知错误'));
        }
    },

    async delete(id) {
        console.log('[delete] 删除模板:', id);
        const confirmed = await showConfirmDialog('确定要删除这个模板吗？');
        if (!confirmed) {
            console.log('[delete] 用户取消');
            return;
        }
        const result = await TemplateApi.delete(id);
        console.log('[delete] API返回:', result);
        if (result.success) {
            TemplateState.currentTemplate = null;
            await this.loadTemplates();
        } else {
            await showAlertDialog('删除失败: ' + (result.message || '未知错误'));
        }
    },

    renderVariables() {
        const container = document.getElementById('variableList');
        const vars = TemplateState.variables[TemplateState.currentType] || [];
        if (!vars.length) {
            container.innerHTML = '<div class="empty-state">暂无变量</div>';
            return;
        }
        container.innerHTML = vars.map(v => `
            <div class="variable-item" onclick="insertVariable('${v.name}')">
                <span class="var-name">{{${v.name}}}</span>
                <span class="var-desc">${v.displayName || ''}</span>
            </div>
        `).join('');
    },

    async select(id) {
        const template = await TemplateApi.get(id);
        if (!template) return;
        TemplateState.currentTemplate = template;
        this.renderList();
        this.loadToEditor();
        document.getElementById('editMain').style.display = 'flex';
    },

    loadToEditor() {
        const { currentTemplate, currentType } = TemplateState;
        document.getElementById('templateName').value = currentTemplate?.name || '';
        document.getElementById('templateIdDisplay').textContent = currentTemplate?.id || '自动生成';
        const field = currentType + 'Template';
        document.getElementById('editorContent').value = currentTemplate?.[field] || '';
        this.renderVariables();
    },

    switchType(type) {
        this.saveCurrentContent();
        TemplateState.currentType = type;
        document.querySelectorAll('.template-tabs .tab').forEach(tab => {
            tab.classList.toggle('active', tab.dataset.type === type);
        });
        this.loadToEditor();
    },

    saveCurrentContent() {
        const { currentTemplate, currentType } = TemplateState;
        if (!currentTemplate) return;
        const field = currentType + 'Template';
        currentTemplate[field] = document.getElementById('editorContent').value;
    }
};
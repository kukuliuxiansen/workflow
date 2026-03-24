/**
 * 模板配置 - 事件处理模块
 */

// @符号变量下拉
const VarDropdown = {
    show(textarea, atIndex) {
        const dropdown = document.getElementById('varDropdown');
        const vars = TemplateState.variables[TemplateState.currentType] || [];
        if (!vars.length) return;

        const rect = textarea.getBoundingClientRect();
        const lineHeight = 18;
        const lines = textarea.value.substring(0, atIndex).split('\n');
        const lineNum = lines.length;
        const colNum = lines[lines.length - 1].length;
        const top = rect.top + (lineNum * lineHeight) - textarea.scrollTop + 20;
        const left = rect.left + Math.min(colNum * 7, rect.width - 150);

        dropdown.innerHTML = vars.map(v => `
            <div class="dropdown-item" onclick="event.stopPropagation();VarDropdown.insert('${v.name}')">
                <span class="var-name">{{${v.name}}}</span>
                <span class="var-desc">${v.displayName || ''}</span>
            </div>
        `).join('');
        dropdown.style.top = top + 'px';
        dropdown.style.left = left + 'px';
        dropdown.classList.add('show');

        TemplateState.dropdownAt = atIndex;
    },

    hide() {
        document.getElementById('varDropdown').classList.remove('show');
        TemplateState.dropdownAt = null;
    },

    insert(varName) {
        const textarea = document.getElementById('editorContent');
        const atPos = TemplateState.dropdownAt;
        if (atPos === null) return;

        const scrollTop = textarea.scrollTop;
        const text = textarea.value;
        const before = text.substring(0, atPos);
        const after = text.substring(atPos + 1);
        const insertText = `{{${varName}}}`;

        textarea.value = before + insertText + after;
        const newPos = atPos + insertText.length;
        textarea.setSelectionRange(newPos, newPos);
        textarea.focus();
        textarea.scrollTop = scrollTop;

        this.hide();
    }
};

document.addEventListener('DOMContentLoaded', () => {
    console.log('[template-config] DOMContentLoaded, 开始初始化...');
    TemplateAction.init();
    console.log('[template-config] 初始化完成');
});

document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('varDropdown');
    if (dropdown && !dropdown.contains(e.target)) {
        VarDropdown.hide();
    }
});

window.createTemplate = async function() {
    console.log('[createTemplate] 开始执行');
    const name = await showInputDialog('请输入模板名称:', '新模板');
    console.log('[createTemplate] 输入返回值:', name, '类型:', typeof name);

    if (!name) {
        console.log('[createTemplate] 用户取消或未输入，退出');
        return;
    }

    console.log('[createTemplate] 准备创建模板:', name);
    const result = await TemplateApi.create({ name, isDefault: false });
    console.log('[createTemplate] API 返回:', result);

    if (result.success) {
        await TemplateAction.loadTemplates();
        await TemplateAction.select(result.data.id);
        console.log('[createTemplate] 创建成功');
    } else {
        await showAlertDialog('创建失败: ' + result.message);
    }
};

window.saveTemplate = async function() {
    const { currentTemplate } = TemplateState;
    if (!currentTemplate) return;
    TemplateAction.saveCurrentContent();
    currentTemplate.name = document.getElementById('templateName').value;
    const result = await TemplateApi.update(currentTemplate.id, currentTemplate);
    if (result.success) {
        await showAlertDialog('保存成功');
        await TemplateAction.loadTemplates();
    } else {
        await showAlertDialog('保存失败: ' + result.message);
    }
};

window.switchTab = function(type) {
    TemplateAction.switchType(type);
};

window.onEditorKeydown = function(e) {
    const textarea = e.target;
    if (e.key === '@') {
        const pos = textarea.selectionStart;
        setTimeout(() => VarDropdown.show(textarea, pos), 0);
    } else if (e.key === 'Escape') {
        VarDropdown.hide();
    }
};

window.insertVariable = function(name) {
    const textarea = document.getElementById('editorContent');
    const scrollTop = textarea.scrollTop;
    const pos = textarea.selectionStart;
    const text = textarea.value;
    const insertText = `{{${name}}}`;
    textarea.value = text.substring(0, pos) + insertText + text.substring(pos);
    textarea.setSelectionRange(pos + insertText.length, pos + insertText.length);
    textarea.focus();
    textarea.scrollTop = scrollTop;
};

window.showPreview = async function() {
    const { currentType, variables } = TemplateState;
    const template = document.getElementById('editorContent').value;
    const vars = variables[currentType] || [];

    const varsContainer = document.getElementById('previewVars');
    varsContainer.innerHTML = vars.map(v => `
        <input type="text" placeholder="${v.displayName || v.name}" data-var="${v.name}" value="">
    `).join('');
    varsContainer.innerHTML += '<button class="btn btn-primary btn-sm" onclick="runPreview()">预览</button>';
    varsContainer.dataset.template = template;

    document.getElementById('previewModal').classList.add('show');
    document.getElementById('previewOutput').textContent = '请填写变量值后点击预览';
};

window.runPreview = async function() {
    const varsContainer = document.getElementById('previewVars');
    const template = varsContainer.dataset.template;
    const inputs = varsContainer.querySelectorAll('input[data-var]');
    const vars = {};
    inputs.forEach(input => { vars[input.dataset.var] = input.value || `{{${input.dataset.var}}}`; });
    const result = await TemplateApi.preview(template, vars);
    document.getElementById('previewOutput').textContent = result;
};

window.closeModal = function() {
    document.getElementById('previewModal').classList.remove('show');
};

window.VarDropdown = VarDropdown;
window.TemplateAction = TemplateAction;
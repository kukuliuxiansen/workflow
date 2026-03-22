/**
 * 模板配置 - API模块
 */
const TemplateApi = {
    async list() {
        const res = await fetch('/api/smart-templates');
        const data = await res.json();
        return data.success ? data.data : [];
    },

    async get(id) {
        const res = await fetch(`/api/smart-templates/${id}`);
        const data = await res.json();
        return data.success ? data.data : null;
    },

    async create(template) {
        const res = await fetch('/api/smart-templates', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(template)
        });
        return await res.json();
    },

    async update(id, template) {
        const res = await fetch(`/api/smart-templates/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(template)
        });
        return await res.json();
    },

    async delete(id) {
        const res = await fetch(`/api/smart-templates/${id}`, { method: 'DELETE' });
        return await res.json();
    },

    async setDefault(id) {
        const res = await fetch(`/api/smart-templates/${id}/set-default`, { method: 'PUT' });
        return await res.json();
    },

    async copy(id) {
        const res = await fetch(`/api/smart-templates/${id}/copy`, { method: 'POST' });
        return await res.json();
    },

    async getVariables(type) {
        const res = await fetch(`/api/smart-templates/variables?type=${type}`);
        const data = await res.json();
        return data.success ? data.data : [];
    },

    async preview(template, variables) {
        const res = await fetch('/api/smart-templates/preview', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ template, variables })
        });
        const data = await res.json();
        return data.success ? data.data : '';
    }
};

window.TemplateApi = TemplateApi;
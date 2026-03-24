/**
 * 模板配置 - 弹窗模块
 */
window.showInputDialog = function(message, defaultValue) {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay show';
        overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);display:flex;justify-content:center;align-items:center;z-index:9999;';

        const modal = document.createElement('div');
        modal.style.cssText = 'background:#252525;border-radius:8px;border:1px solid #3a3a3a;width:400px;padding:20px;';
        modal.innerHTML = `
            <div style="color:#e0e0e0;margin-bottom:16px;font-size:14px;">${message}</div>
            <input type="text" id="dialogInput" value="${defaultValue || ''}" style="width:100%;padding:8px 12px;background:#1a1a1a;border:1px solid #3a3a3a;border-radius:4px;color:#e0e0e0;font-size:13px;outline:none;margin-bottom:16px;">
            <div style="display:flex;justify-content:flex-end;gap:8px;">
                <button id="dialogCancel" style="padding:8px 16px;background:#3a3a3a;border:none;border-radius:4px;color:#aaa;font-size:13px;cursor:pointer;">取消</button>
                <button id="dialogOk" style="padding:8px 16px;background:#6b5ce7;border:none;border-radius:4px;color:#fff;font-size:13px;cursor:pointer;">确定</button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        const input = modal.querySelector('#dialogInput');
        const cancelBtn = modal.querySelector('#dialogCancel');
        const okBtn = modal.querySelector('#dialogOk');

        input.focus();
        input.select();

        const close = (value) => {
            document.body.removeChild(overlay);
            resolve(value);
        };

        cancelBtn.onclick = () => close(null);
        okBtn.onclick = () => close(input.value);
        input.onkeydown = (e) => {
            if (e.key === 'Enter') close(input.value);
            if (e.key === 'Escape') close(null);
        };
        overlay.onclick = (e) => {
            if (e.target === overlay) close(null);
        };
    });
};

window.showConfirmDialog = function(message) {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay show';
        overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);display:flex;justify-content:center;align-items:center;z-index:9999;';

        const modal = document.createElement('div');
        modal.style.cssText = 'background:#252525;border-radius:8px;border:1px solid #3a3a3a;width:360px;padding:20px;';
        modal.innerHTML = `
            <div style="color:#e0e0e0;margin-bottom:20px;font-size:14px;">${message}</div>
            <div style="display:flex;justify-content:flex-end;gap:8px;">
                <button id="confirmCancel" style="padding:8px 16px;background:#3a3a3a;border:none;border-radius:4px;color:#aaa;font-size:13px;cursor:pointer;">取消</button>
                <button id="confirmOk" style="padding:8px 16px;background:#dc2626;border:none;border-radius:4px;color:#fff;font-size:13px;cursor:pointer;">确定</button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        const cancelBtn = modal.querySelector('#confirmCancel');
        const okBtn = modal.querySelector('#confirmOk');

        const close = (value) => {
            document.body.removeChild(overlay);
            resolve(value);
        };

        cancelBtn.onclick = () => close(false);
        okBtn.onclick = () => close(true);
        okBtn.focus();
        overlay.onclick = (e) => {
            if (e.target === overlay) close(false);
        };
    });
};

window.showAlertDialog = function(message) {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay show';
        overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);display:flex;justify-content:center;align-items:center;z-index:9999;';

        const modal = document.createElement('div');
        modal.style.cssText = 'background:#252525;border-radius:8px;border:1px solid #3a3a3a;width:360px;padding:20px;';
        modal.innerHTML = `
            <div style="color:#e0e0e0;margin-bottom:20px;font-size:14px;">${message}</div>
            <div style="display:flex;justify-content:flex-end;">
                <button id="alertOk" style="padding:8px 16px;background:#6b5ce7;border:none;border-radius:4px;color:#fff;font-size:13px;cursor:pointer;">确定</button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        const okBtn = modal.querySelector('#alertOk');

        const close = () => {
            document.body.removeChild(overlay);
            resolve();
        };

        okBtn.onclick = close;
        okBtn.focus();
        overlay.onclick = (e) => {
            if (e.target === overlay) close();
        };
    });
};
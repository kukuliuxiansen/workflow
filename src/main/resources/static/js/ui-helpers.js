// Toast 提示
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
    function confirmAsync(message) {
      return new Promise(resolve => {
        showConfirm(message, () => resolve(true), () => resolve(false));
      });
    }
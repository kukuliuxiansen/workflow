// ========== API日志拦截器 ==========
const originalFetch = window.fetch;
window.fetch = async function(url, options = {}) {
  const startTime = Date.now();
  const method = options.method || 'GET';
  const requestTime = new Date().toLocaleTimeString();
  const traceId = 'TRC-' + Date.now() + '-' + Math.random().toString(16).substr(2, 4).toUpperCase();

  let requestPreview = '';
  if (options.body) {
    try {
      const bodyStr = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
      requestPreview = bodyStr.length > 500 ? bodyStr.substring(0, 500) + '...(truncated)' : bodyStr;
    } catch (e) {
      requestPreview = '[无法序列化]';
    }
  }

  try {
    const response = await originalFetch.call(this, url, options);
    const duration = Date.now() - startTime;
    const clonedResponse = response.clone();

    let responsePreview = '';
    try {
      const responseText = await clonedResponse.text();
      responsePreview = responseText.length > 500 ? responseText.substring(0, 500) + '...(truncated)' : responseText;
    } catch (e) {
      responsePreview = '[无法读取响应]';
    }

    if (typeof url === 'string' && url.startsWith(API) && state.logs && state.logs.api) {
      const logEntry = {
        traceId, time: requestTime, method,
        url: url.replace(API, ''),
        status: response.status,
        duration: duration + 'ms',
        request: requestPreview,
        response: responsePreview,
        success: response.ok
      };
      state.logs.api.unshift(logEntry);
      if (state.logs.api.length > 200) {
        state.logs.api = state.logs.api.slice(0, 200);
      }
      updateApiLogCount();
    }
    return response;
  } catch (error) {
    const duration = Date.now() - startTime;
    if (typeof url === 'string' && url.startsWith(API) && state.logs && state.logs.api) {
      const logEntry = {
        traceId, time: requestTime, method,
        url: url.replace(API, ''),
        status: 'ERROR',
        duration: duration + 'ms',
        request: requestPreview,
        response: error.message,
        success: false
      };
      state.logs.api.unshift(logEntry);
      updateApiLogCount();
    }
    throw error;
  }
};

function updateApiLogCount() {
  const el = document.getElementById('apiLogCount');
  if (el && state.logs && state.logs.api) el.textContent = state.logs.api.length;
}

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
      </div>
    `;
  }).join('');
  container.innerHTML = html;
}
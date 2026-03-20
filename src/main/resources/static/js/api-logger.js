    // 更新API日志计数
    function updateApiLogCount() {
      const el = document.getElementById('apiLogCount');
      if (el) {
        el.textContent = state.logs.api.length;
      }
    }

    const originalFetch = window.fetch;
    window.fetch = async function(url, options = {}) {
      const startTime = Date.now();
      const method = options.method || 'GET';
      const requestTime = new Date().toLocaleTimeString();
      const traceId = 'TRC-' + Date.now() + '-' + Math.random().toString(16).substr(2, 4).toUpperCase();

      // 记录请求
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

        // 克隆response以便读取内容
        const clonedResponse = response.clone();

        // 记录响应
        let responsePreview = '';
        try {
          const responseText = await clonedResponse.text();
          responsePreview = responseText.length > 500 ? responseText.substring(0, 500) + '...(truncated)' : responseText;
        } catch (e) {
          responsePreview = '[无法读取响应]';
        }

        // 只记录API调用
        if (typeof url === 'string' && url.startsWith(API)) {
          // 确保logs.api已初始化
          if (!state.logs) state.logs = { execution: [], agent: [], api: [] };
          if (!state.logs.api) state.logs.api = [];

          const logEntry = {
            traceId,
            time: requestTime,
            method,
            url: url.replace(API, ''),
            status: response.status,
            duration: duration + 'ms',
            request: requestPreview,
            response: responsePreview,
            success: response.ok
          };

          state.logs.api.unshift(logEntry);
          // 限制API日志数量
          if (state.logs.api.length > 200) {
            state.logs.api = state.logs.api.slice(0, 200);
          }
          updateApiLogCount();
        }

        return response;
      } catch (error) {
        const duration = Date.now() - startTime;

        // 记录错误
        if (typeof url === 'string' && url.startsWith(API)) {
          // 确保logs.api已初始化
          if (!state.logs) state.logs = { execution: [], agent: [], api: [] };
          if (!state.logs.api) state.logs.api = [];

          const logEntry = {
            traceId,
            time: requestTime,
            method,
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


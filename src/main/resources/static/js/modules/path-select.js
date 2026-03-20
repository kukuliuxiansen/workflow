// ========== 路径选择模态框功能 ==========

function openPathSelectModal(options) {
  state.pathSelect = {
    mode: options.mode || 'file',
    targetId: options.targetId || '',
    callback: options.onSelect || null,
    currentPath: '/',
    filters: options.filters || [],
    showLocalUpload: options.showLocalUpload !== false,
    localFileAccept: options.localFileAccept || '',
    onLocalUpload: options.onLocalUpload || null
  };

  const pathSelectTitle = document.getElementById('pathSelectTitle');
  if (pathSelectTitle) pathSelectTitle.textContent = options.title || '📁 选择路径';
  const pathManualInput = document.getElementById('pathManualInput');
  if (pathManualInput) {
    pathManualInput.value = '';
    pathManualInput.placeholder = options.mode === 'directory' ? '输入目录完整路径...' : '输入文件完整路径...';
  }

  const localUploadGroup = document.getElementById('pathLocalUploadGroup');
  if (state.pathSelect.showLocalUpload && options.mode === 'file') {
    localUploadGroup.style.display = 'block';
    document.getElementById('pathLocalFile').accept = state.pathSelect.localFileAccept;
  } else {
    localUploadGroup.style.display = 'none';
  }

  document.getElementById('pathSelectModal').classList.add('show');
  browseDirectory('/');
}

function confirmManualPath() {
  const path = document.getElementById('pathManualInput').value.trim();
  if (!path) {
    showToast('warn', '请输入路径');
    return;
  }
  if (state.pathSelect.targetId) {
    document.getElementById(state.pathSelect.targetId).value = path;
  }
  closeModal('pathSelectModal');
  if (state.pathSelect.callback) {
    state.pathSelect.callback(path);
  }
}

async function browseDirectory(dirPath) {
  state.pathSelect.currentPath = dirPath;
  document.getElementById('currentBrowsePath').value = dirPath;

  const content = document.getElementById('pathBrowserContent');
  content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

  try {
    const res = await fetch(`${API}/system/browse-directory`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path: dirPath, mode: state.pathSelect.mode, filters: state.pathSelect.filters })
    });
    const data = await res.json();

    if (data.success) {
      renderDirectoryContents(data.items);
    } else {
      content.innerHTML = `<div style="padding:20px;text-align:center;color:#f66;">${data.error || '无法读取目录'}</div>`;
    }
  } catch (e) {
    content.innerHTML = `<div style="padding:20px;text-align:center;color:#f66;">请求失败: ${e.message}</div>`;
  }
}

function renderDirectoryContents(items) {
  const content = document.getElementById('pathBrowserContent');
  if (!items || items.length === 0) {
    content.innerHTML = '<div style="padding:20px;text-align:center;color:#666;">空目录</div>';
    return;
  }
  const html = items.map(item => {
    const icon = item.isDirectory ? '📁' : '📄';
    const itemClass = item.isDirectory ? 'path-item dir' : 'path-item file';
    const clickHandler = item.isDirectory
      ? `onclick="browseDirectory('${item.path}')"`
      : `onclick="selectPath('${item.path}')"`;
    return `<div class="${itemClass}" ${clickHandler}>
      <span class="icon">${icon}</span>
      <span class="name">${item.name}</span>
    </div>`;
  }).join('');
  content.innerHTML = html;
}

function browseParentDir() {
  const current = state.pathSelect.currentPath;
  if (current === '/' || !current) return;
  const parts = current.split('/').filter(p => p);
  parts.pop();
  const parent = parts.length === 0 ? '/' : '/' + parts.join('/');
  browseDirectory(parent);
}

function refreshCurrentDir() {
  browseDirectory(state.pathSelect.currentPath);
}

function selectPath(path) {
  if (state.pathSelect.mode === 'directory') {
    showToast('warn', '请选择一个目录（点击进入子目录，或手动输入路径）');
    return;
  }
  if (state.pathSelect.targetId) {
    document.getElementById(state.pathSelect.targetId).value = path;
  }
  closeModal('pathSelectModal');
  if (state.pathSelect.callback) {
    state.pathSelect.callback(path);
  }
}
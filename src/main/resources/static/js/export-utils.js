// 导出功能

    function exportCanvasAsImage() {
      showToast('info', '正在导出图片...');
      const content = document.getElementById('canvasContent');
      const canvas = document.createElement('canvas');
      const rect = content.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;
      showToast('warn', '图片导出功能开发中，请使用浏览器截图');
    }

    function exportWorkflowAsJson() {
      if (!state.currentWorkflow) return;

      const dataStr = JSON.stringify(state.currentWorkflow, null, 2);
      const blob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = `${state.currentWorkflow.name || 'workflow'}.json`;
      a.click();

      URL.revokeObjectURL(url);
      showToast('success', '工作流已导出');
    }
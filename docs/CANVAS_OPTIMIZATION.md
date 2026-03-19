# 工作流画布优化文档

## 📅 优化日期: 2026-03-20

---

## 🚀 十大改动 (Major Improvements)

### 1. 框选多选功能 (Multi-Select with Selection Box)

**实现方式**: 使用 Vue Flow 的 `selection-on-drag` 和 `selection-mode` 属性

**代码位置**: `Canvas.vue`
```typescript
const selectionOnDrag = computed(() => !editorStore.isPanning && !canvasLocked.value)
const selectionMode = ref(SelectionMode.Partial)
```

**功能说明**:
- 拖动鼠标可框选多个节点
- 支持部分选择模式
- 配合 Ctrl 键可添加/移除选择

**使用场景**: 批量移动、删除、对齐多个节点

---

### 2. Ctrl + 鼠标拖动画布 (Pan Canvas with Ctrl+Drag)

**实现方式**: 动态控制 `panOnDrag` 属性

**代码位置**: `Canvas.vue`
```typescript
const panOnDrag = computed(() => editorStore.isPanning || canvasLocked.value ? [1, 2] : false)
```

**功能说明**:
- 默认拖动为框选模式
- 按住 Ctrl 键拖动可平移画布
- 画布锁定时自动切换为平移模式

**使用场景**: 大型工作流导航、精确定位

---

### 3. 右键菜单系统 (Context Menu System)

**实现方式**: 自定义右键菜单组件，支持节点、连线、画布三种上下文

**代码位置**: `Canvas.vue` 模板部分

**功能说明**:

**节点右键菜单**:
- 复制节点 (Ctrl+C)
- 粘贴节点 (Ctrl+V)
- 复制为副本 (Ctrl+D)
- 全选节点 (Ctrl+A)
- 反选节点
- 左/中/右对齐
- 水平/垂直分布
- 置于顶层/底层
- 节点颜色选择
- 删除节点 (Delete)

**连线右键菜单**:
- 编辑标签
- 切换类型（成功/失败）
- 删除连线

**画布右键菜单**:
- 粘贴节点
- 全选节点
- 开启/关闭网格对齐
- 锁定/解锁画布
- 适应画布
- 重置视图
- 导出为图片

---

### 4. 节点颜色自定义 (Custom Node Colors)

**实现方式**: 通过 `nodeColorMap` 状态管理节点颜色，CSS 变量动态绑定

**代码位置**:
- `Canvas.vue`: `setNodeColor()` 方法
- `Node.vue`: `nodeStyle` 和 `iconStyle` 计算属性

**功能说明**:
- 右键菜单选择 10 种预设颜色
- 颜色应用于节点边框和图标背景
- 支持批量设置多个选中节点的颜色

**颜色预设**:
```javascript
const nodeColors = ref([
  '#f38ba8', '#fab387', '#f9e2af', '#a6e3a1', '#89dceb',
  '#89b4fa', '#cba6f7', '#f5c2e7', '#94e2d5', '#74c7ec'
])
```

---

### 5. 撤销/重做系统 (Undo/Redo System)

**实现方式**: 维护历史状态栈，每次操作保存快照

**代码位置**: `Canvas.vue`
```typescript
interface HistoryState {
  nodes: WorkflowNode[]
  edges: any[]
}
const history = ref<HistoryState[]>([])
const historyIndex = ref(-1)
const maxHistory = 50

function saveHistory() { /* ... */ }
function undo() { /* ... */ }
function redo() { /* ... */ }
```

**功能说明**:
- 最多保存 50 步历史记录
- 支持 Ctrl+Z 撤销、Ctrl+Y/Ctrl+Shift+Z 重做
- 节点拖拽、添加、删除、连线操作都会记录

---

### 6. 复制/粘贴节点 (Copy/Paste Nodes)

**实现方式**: 使用剪贴板状态存储选中的节点

**代码位置**: `Canvas.vue`
```typescript
const clipboard = ref<WorkflowNode[]>([])

function copySelectedNodes() { /* ... */ }
function pasteNodes() { /* ... */ }
function duplicateSelectedNodes() { /* ... */ }
```

**功能说明**:
- Ctrl+C 复制选中节点
- Ctrl+V 粘贴节点（带偏移量防止重叠）
- Ctrl+D 快速复制为副本
- 粘贴时自动添加 "(副本)" 后缀

---

### 7. 节点搜索功能 (Node Search)

**实现方式**: 搜索面板组件，实时过滤节点列表

**代码位置**: `Canvas.vue` 模板部分

**功能说明**:
- Ctrl+F 打开搜索面板
- 实时搜索节点名称
- 显示节点类型标签
- 点击结果定位并选中节点
- 搜索结果高亮当前选中项

**界面设计**:
- 顶部搜索输入框
- 节点类型彩色标签
- ESC 关闭搜索

---

### 8. 画布网格对齐增强 (Enhanced Grid Snapping)

**实现方式**: Vue Flow 的 `snap-to-grid` 和 `snap-grid` 属性

**代码位置**: `Canvas.vue`
```typescript
const snapToGrid = ref(true)
const gridSize = ref(20)
```

**功能说明**:
- 默认开启网格对齐
- 可通过工具栏按钮或右键菜单开关
- 网格大小 20px
- 拖动时自动吸附到网格点

**对齐辅助线** (额外增强):
```typescript
const showAlignmentGuides = ref(true)
const alignmentGuides = ref<{ id: string; orientation: string; position: number }[]>([])
```
- 拖动节点时显示对齐辅助线
- 水平/垂直居中对齐提示
- 左/右边缘对齐提示

---

### 9. 节点注释/备注 (Node Annotations)

**实现方式**: 在节点配置中添加 description 字段

**代码位置**: `Node.vue`
```typescript
const description = computed(() => config.value?.description || '')
```

**功能说明**:
- 节点内容区显示描述文本
- 最多显示两行，超出省略
- 灰色字体区分主标题

---

### 10. 快捷键系统 (Keyboard Shortcuts System)

**实现方式**: 全局键盘事件监听

**代码位置**:
- `Canvas.vue`: `onKeyDown()` 方法
- `WorkflowEditor.vue`: `onKeyDown()` 方法

**完整快捷键列表**:

| 快捷键 | 功能 | 作用域 |
|--------|------|--------|
| Ctrl+C | 复制节点 | 画布 |
| Ctrl+V | 粘贴节点 | 画布 |
| Ctrl+D | 复制为副本 | 画布 |
| Ctrl+A | 全选节点 | 画布 |
| Ctrl+Z | 撤销 | 画布 |
| Ctrl+Y | 重做 | 画布 |
| Ctrl+Shift+Z | 重做 | 画布 |
| Ctrl+F | 搜索节点 | 画布 |
| Delete | 删除选中 | 画布 |
| Backspace | 删除选中 | 画布 |
| Escape | 取消选择/关闭 | 全局 |
| Ctrl+S | 保存工作流 | 编辑器 |
| Ctrl++ | 放大 | 编辑器 |
| Ctrl+- | 缩小 | 编辑器 |
| Ctrl+0 | 适应画布 | 编辑器 |
| ? | 显示帮助 | 编辑器 |

---

## 🎨 二十小优化 (Minor Improvements)

### 1. 节点悬停工具栏
- 鼠标悬停显示编辑、复制、删除按钮
- 平滑淡入淡出动画

### 2. 连线动画增强
- 默认开启流动动画
- 选中状态加粗显示
- 不同类型不同颜色（成功绿色、失败红色）

### 3. 节点缩放动画
- 选中时轻微放大 (1.02x)
- 悬停时上浮效果

### 4. 画布缩放显示百分比
- 工具栏实时显示缩放比例
- 支持范围：10% - 400%

### 5. 节点对齐辅助线
- 拖动时显示紫色辅助线
- 自动检测居中、边缘对齐

### 6. 节点分布排列
- 右键菜单支持水平/垂直均匀分布
- 适用于 3 个以上节点

### 7. 导出为图片
- 使用 html-to-image 库
- 一键导出 PNG 格式

### 8. 节点折叠/展开
- 内容区可折叠
- 折叠按钮悬停显示

### 9. 连线标签编辑
- 右键菜单可编辑连线标签
- 支持切换连线类型

### 10. 节点状态指示器
- 运行中：蓝色脉冲动画
- 已完成：绿色勾选图标
- 错误：红色叉号图标

### 11. 小地图增强
- 不同节点类型不同颜色
- 支持点击定位
- 可通过工具栏开关显示

### 12. 画布背景自定义
- 使用点状背景
- 背景颜色跟随主题

### 13. 节点拖拽预览
- 拖动时显示半透明预览
- 实时更新位置

### 14. 连接点高亮提示
- 悬停时放大 1.3 倍
- 显示发光效果

### 15. 节点工具提示
- 显示节点名称、类型、连接数
- 跟随鼠标位置

### 16. 画布锁定功能
- 锁定后禁止编辑操作
- 工具栏显示锁定图标

### 17. 节点层级操作
- 置于顶层/底层
- 调整节点叠加顺序

### 18. 批量节点操作
- 支持多选后批量删除
- 批量修改颜色

### 19. 状态保存提示
- 未保存时显示提示标签
- 页面离开前确认

### 20. 节点统计显示
- 左下角显示节点和连线数量
- 实时更新

---

## 🔧 技术实现细节

### 依赖库更新

```json
{
  "@vue-flow/core": "^1.x",
  "@vue-flow/background": "^1.x",
  "@vue-flow/controls": "^1.x",
  "@vue-flow/minimap": "^1.x",
  "html-to-image": "^1.x"
}
```

### 状态管理

新增 Editor Store 状态:
```typescript
{
  isPanning: boolean,
  gridSize: number,
  snapToGrid: boolean,
  showGrid: boolean,
  canvasLocked: boolean,
  showMiniMap: boolean
}
```

### CSS 变量

新增主题变量:
```css
--node-color: #313244;  /* 节点默认边框颜色 */
```

---

## 📊 性能优化

1. **事件节流**: 拖动和缩放事件使用节流处理
2. **虚拟滚动**: 搜索结果列表限制高度
3. **按需渲染**: 对齐辅助线仅在拖动时计算
4. **内存管理**: 历史记录限制 50 条

---

## 📝 使用说明

### 基本操作

1. **添加节点**: 从左侧面板拖拽到画布
2. **连接节点**: 拖动节点连接点到另一节点
3. **选择节点**: 点击选中，Shift+点击多选，或拖动框选
4. **移动节点**: 拖动节点到目标位置
5. **编辑属性**: 选中节点后在右侧面板编辑

### 高级操作

1. **批量操作**: 框选多个节点后使用右键菜单
2. **快速定位**: Ctrl+F 搜索，点击结果跳转
3. **撤销操作**: Ctrl+Z 撤销，Ctrl+Y 重做
4. **导出分享**: 点击"导出图片"生成 PNG

---

## 🐛 已知问题 & 解决方案

| 问题 | 解决方案 |
|------|----------|
| 右键菜单位置溢出 | 自动调整到可视区域内 |
| 快速操作历史栈溢出 | 限制最大 50 条记录 |
| 节点重叠 | 粘贴时添加偏移量 |

---

## 🚧 未来计划

1. 节点分组功能
2. 画布缩略图导航
3. 更多节点类型
4. 连线路径类型选择（直线/曲线/折线）
5. 节点模板系统

---

**文档版本**: 1.0
**最后更新**: 2026-03-20
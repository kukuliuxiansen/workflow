| 类别 | 命令 | 功能描述 | 使用场景 | 使用方法与示例 |
|:---|:---|:---|:---|:---|
| 基础控制类 | `/help` | 显示所有可用命令列表及说明 | 忘记命令时查询帮助 | `/help` 或 `/help [command]` |
| 基础控制类 | `/exit` | 退出当前会话 | 完成工作后离开 | `/exit` |
| 基础控制类 | `/quit` | 退出当前会话（同 exit） | 完成工作后离开 | `/quit` |
| 基础控制类 | `/clear` | 清除对话历史（保留代码改动） | 上下文太长、回复变慢时 | `/clear` 或 `/clear reset` |
| 基础控制类 | `/compact` | 压缩会话内容，节省 tokens | 想保留上下文但释放内存时 | `/compact [说明]` |
| 会话管理类 | `/status` | 查看状态（版本、模型、账户、连接） | 检查当前配置和连接状态 | `/status` |
| 会话管理类 | `/history` | 查看会话历史记录 | 回顾之前的对话内容 | `/history` |
| 会话管理类 | `/resume` | 恢复之前的会话 | 继续之前中断的工作 | `/resume [session-id]` |
| 会话管理类 | `/save` | 保存当前会话状态到文件 | 需要备份当前进度时 | `/save [filename]` |
| 会话管理类 | `/load` | 从文件加载会话状态 | 恢复之前保存的会话 | `/load [filename]` |
| 任务管理类 | `/todo` | 任务管理核心（显示/添加/完成） | 跟踪和管理待办事项 | `/todo` 显示列表 |
| 任务管理类 | `/todos` | 列举当前所有待办项 | 快速查看所有任务 | `/todos` |
| 任务管理类 | `/todo add` | 添加新任务 | 创建新的待办事项 | `/todo add 修复登录 bug` |
| 任务管理类 | `/todo complete` | 标记任务为已完成 | 完成任务后更新状态 | `/todo complete <ID>` |
| 任务管理类 | `/todo start` | 标记任务为进行中 | 开始执行某个任务 | `/todo start <ID>` |
| 任务管理类 | `/todo update` | 修改任务描述 | 任务内容需要变更时 | `/todo update <ID> <新描述>` |
| 任务管理类 | `/todo remove` | 删除任务 | 取消不再需要的任务 | `/todo remove <ID>` |
| 项目管理类 | `/init` | 扫描项目并生成 CLAUDE.md | 新项目初始化 | `/init` |
| 项目管理类 | `/add-dir` | 添加额外工作目录供 Claude 访问 | 多项目协作时 | `/add-dir ../shared-lib` |
| 项目管理类 | `/memory` | 编辑项目记忆文件（CLAUDE.md） | 更新项目规范和约定 | `/memory` |
| 项目管理类 | `/pr_comments` | 查看 GitHub Pull Request 评论 | Code Review 时 | `/pr_comments` |
| 配置与认证类 | `/config` | 查看或修改配置项（主题、模型等） | 调整个人偏好设置 | `/config` |
| 配置与认证类 | `/login` | 登录账户 | 首次使用或切换账号 | `/login` |
| 配置与认证类 | `/logout` | 登出账户 | 退出当前账号 | `/logout` |
| 配置与认证类 | `/auth` | 管理认证信息（切换账号等） | 多账号管理 | `/auth` |
| 配置与认证类 | `/theme` | 更改终端主题 | 调整界面显示风格 | `/theme [主题名]` |
| 模型与输出类 | `/model` | 切换 AI 模型 | 需要不同能力的模型时 | `/model [模型名]` |
| 模型与输出类 | `/vim` | 开启极客编辑模式 | 习惯 vim 操作的用户 | `/vim` |
| 工具与权限类 | `/permissions` | 管理工具权限 | 控制 AI 可执行的操作 | `/permissions` |
| 工具与权限类 | `/tools` | 查看可用工具列表 | 了解 AI 能做什么 | `/tools` |
| 工具与权限类 | `/mcp` | 管理 MCP 服务器连接 | 连接外部数据源 | `/mcp` |
| 工具与权限类 | `/cost` | 查看 token 使用统计 | 监控使用量和费用 | `/cost` |
| 诊断与报告类 | `/doctor` | 检查 Claude Code 安装状态 | 遇到问题时诊断 | `/doctor` |
| 诊断与报告类 | `/bug` | 上报错误给 Anthropic | 发现 bug 需要反馈 | `/bug <说明>` |
| 代理与技能类 | `/agents` | 管理智能体配置 | 使用专门功能的 Agent | `/agents` |
| 代理与技能类 | `/skills` | 列举可用技能 | 查看已加载的专业能力 | `/skills` |
| 开发与调试类 | `/test` | 运行项目测试套件 | 验证代码修改是否正确 | `/test` |
| 开发与调试类 | `/run` | 显式请求运行 Shell 命令 | 需要执行特定命令时 | `/run <命令>` |
| 开发与调试类 | `/explain` | 解释当前代码块或修改 | 理解代码逻辑时 | `/explain` |
| 开发与调试类 | `/hooks` | 管理钩子功能 | 配置自动化触发器 | `/hooks` |
| 符号命令 | `!` | Bash 模式 - 直接执行 Shell 命令 | 快速执行系统命令 | `!ls -la` |
| 符号命令 | `/` | 命令模式 - 触发斜杠命令 | 使用内置功能 | `/todo add 任务` |
| 符号命令 | `@` | 引用文件/目录 - 注入上下文 | 让 AI 读取特定文件 | `@src/main.ts 请优化` |
| 符号命令 | `&` | 后台运行 - 长任务不阻塞终端 | 执行耗时任务 | `&/todo add 长时间任务` |
| CLI 启动命令 | `claude` | 启动交互式会话 | 开始新会话 | `claude` |
| CLI 启动命令 | `claude [prompt]` | 启动并直接执行指令 | 快速单次任务 | `claude "帮我修复 bug"` |
| CLI 启动命令 | `claude -p [prompt]` | 显式指定 prompt 启动 | 同上，更明确 | `claude -p "写个 hello"` |
| CLI 启动命令 | `claude --version` | 查看版本号 | 检查当前版本 | `claude -v` |
| CLI 启动命令 | `claude --help` | 查看帮助信息 | 获取命令行帮助 | `claude -h` |
| CLI 启动命令 | `claude --config` | 指定配置文件路径 | 使用特定配置 | `claude --config ./config.json` |
| CLI 启动命令 | `claude --model` | 指定模型 | 启动时选择模型 | `claude --model sonnet-4` |
| CLI 启动命令 | `claude --resume` | 恢复之前的会话 | 继续上次工作 | `claude --resume` |
| 快捷键 | `Ctrl + C` | 中断当前生成 或 退出会话 | 停止生成或退出 | 按下即可 |
| 快捷键 | `Ctrl + R` | 重试（重新生成上一次回答） | 对结果不满意时 | 按下即可 |
| 快捷键 | `Ctrl + L` | 清屏（只清理显示，不清理内存） | 终端太乱时 | 按下即可 |
| 快捷键 | `Ctrl + D` | 发送 EOF，等同于退出 | 快速退出 | 按下即可 |
| 快捷键 | `↑` (Up Arrow) | 查看历史输入记录 | 重复使用之前的命令 | 按下即可 |
| 快捷键 | `↓` (Down Arrow) | 查看下一条历史输入 | 浏览历史记录 | 按下即可 |
| 快捷键 | `Tab` | 自动补全（命令/文件名/路径） | 提高输入效率 | 按下即可 |
| 快捷键 | `Esc` | 退出当前菜单或取消操作 | 取消当前操作 | 按下即可 |
| 自定义命令 | 项目级命令 | `.claude/commands/xxx.md` | 当前项目专属命令 | 创建 Markdown 文件定义 |
| 自定义命令 | 用户级命令 | `~/.claude/commands/xxx.md` | 全局可用的自定义命令 | 创建 Markdown 文件定义 |
| 配置文件 | 主配置 | `~/.claude/config.json` | 全局配置（模型、主题等） | 编辑 JSON 文件 |
| 配置文件 | 项目配置 | `.claude/CLAUDE.md` | 项目特定指令和规范 | 编辑 Markdown 文件 |
| 配置文件 | MCP 配置 | `~/.claude/mcp.json` | MCP 服务器连接配置 | 编辑 JSON 文件 |


---
name: ai-agent-ui-black-glass-vue-smart-backend
description: Generate a Vue 3 single-file component implementing a pure-black glassmorphism AI Agent chat UI that auto-adapts to an existing backend chat API by inspecting the project for endpoints, payloads, and response formats. Use when the user wants a production-ready AI Agent web interface that matches their backend’s conversation schema without manual wiring.
---

# AI Agent UI – Black Glass Vue Smart Backend

This skill guides the agent to generate **directly runnable**, **backend-ready** Vue 3 single-file components implementing a **pure黑色毛玻璃 AI Agent 聊天界面**，并且自动适配当前项目的后端接口结构。

## When to Use This Skill

Use this skill when:

- The user wants a **Vue 3 + Composition API + Tailwind CSS (CDN)** single-file component for an AI Agent / Chat UI.
- The UI style should be **纯黑 / 高级毛玻璃 / 极简黑白灰 / 科技感强**。
- The component must **直接对接已有后端服务**，并自动匹配：
  - 会话 ID / 对话上下文
  - 消息发送接口与参数
  - 响应字段格式（例如 `content` / `message` / `role` / `sender` / `stream` 等）
- The user wants **可直接复制进项目并运行** 的 `.vue` 文件，而不是示例伪代码。

---

## Overall Responsibilities

When this skill is active, the agent should:

1. **分析后端**
   - 尽可能阅读与 AI / Chat / Agent / 会话 / 对话 相关的后端代码与接口定义。
   - 自动推断：
     - 主要对话接口地址（如 `/api/chat`, `/chat`, `/api/ai_ops`, `/api/chat_stream` 等）
     - 请求方法（`GET` / `POST`，通常为 `POST`）
     - 请求载荷字段：会话 ID、用户消息内容、额外上下文。
     - 响应结构：消息内容字段、角色字段、时间戳字段、流式 vs 非流式。
   - 在不能完全确定时，**基于项目上下文做出最佳猜测，并在代码中集中注明可配置位置**。

2. **生成前端单文件组件**
   - 输出一个 **完整、单一的 `.vue` 文件**，包含：
     - `<template>`
     - `<script setup lang="ts">` 或 `<script setup>`
     - `<style scoped>` or `<style scoped lang="postcss">` (兼容 Tailwind)
   - 不依赖构建时 Tailwind 插件，**仅使用 Tailwind CSS CDN** 约定的类名。
   - UI 结构需包括：
     - 左侧固定侧边栏：会话列表 + 新建会话按钮
     - 顶部固定导航：标题 + 连接状态 + 当前会话 ID
     - 中间对话区：用户/AI 消息气泡 + 自动滚动
     - 底部输入区：多行输入 + 回车发送 + 按钮发送 + 加载状态

3. **严控 UI 风格**
   - **配色**：
     - 背景：`#000` / `#050505` / `#0a0a0a` 范围
     - 卡片：`#111` / `#1a1a1a` / `#222`
     - 文字：主文本白色 / 次文本灰色（如 `#9ca3af`）
     - 禁止彩色渐变与高饱和色，仅允许极少量状态色（如 `text-emerald-400` 用于“在线”点缀，若使用则要很克制）。
   - **质感**：
     - 使用 `backdrop-blur`, `bg-black/40`, `border-white/5`、`shadow-2xl` 等实现毛玻璃、通透感。
   - **排版**：
     - 默认字体：`font-sans` 假定为 Inter（由外层布局或全局 CSS 配置）。
     - 标题/副标题字重与大小区分明显。
     - 留白充足，避免密集元素堆叠。
   - **动效**：
     - 使用 `transition`, `hover:translate-y-0.5`, `hover:bg-white/10` 等简洁微动效。
     - 禁止复杂动画与夸张动效。

4. **智能对接后端**
   - 自动处理以下逻辑：
     - 会话创建 / 切换：
       - 新建会话时生成本地会话 ID，或从后端响应中读取（如果后端提供）。
       - 切换会话时加载对应历史消息（从后端或本地缓存）。
     - 消息发送：
       - 将当前输入框内容映射为后端预期的字段，如：
         - `question`, `prompt`, `content`, `message` 等。
       - 发送前进行空字符串校验。
       - 显示加载状态（按钮禁用、spinner、顶部连接状态）。
     - 消息接收：
       - 从响应结构中提取 AI 回复内容：
         - 如 `data.content`, `data.answer`, `data.result`, `choices[0].message.content` 等。
       - 根据后端字段区分角色（如 `role`, `sender`, `from`），如果没有显式角色字段，则默认“用户”和“AI”两类。
     - 错误处理：
       - 网络错误、非 2xx 响应、后端返回错误信息。
       - 通过顶部轻量错误条或 Toast 区域展示（小型、不会破坏布局）。
     - 空状态：
       - 当前会话无消息时，显示一个优雅的“空对话”提示卡片。

---

## Step-by-Step Workflow

当用户请求使用本技能时，遵循以下步骤。

### 1. Inspect Backend Project

1. 查找明显的 AI / Chat / Agent 入口：
   - 控制器类：`ChatController`, `AiOpsController`, `AgentController`, `*Chat*Controller`, `*AiOps*Controller` 等。
   - 服务类：`ChatService`, `AiOpsService`, `RagService` 等。
   - API 文档 / Swagger 注解。
2. 推断主要接口：
   - 优先查找类似：
     - `/api/chat`, `/api/chat_stream`, `/api/ai_ops`, `/chat`, `/ai/agent` 等路径。
   - 记录：
     - URL
     - HTTP 方法
     - 请求 DTO 字段（例如 `id`, `question`, `message`, `content` 等）
     - 返回 DTO 字段（例如 `content`, `answer`, `messages`, `role` 等）
3. 如果有 **流式 SSE / WebSocket**：
   - 判断是否需要使用 `EventSource` / `ReadableStream` 等。
   - 如果识别出了 SSE 端点（例如 `/chat_stream`），在前端中实现对应流式读取逻辑。
4. 如果无法准确定位单一端点：
   - 在合理范围内选取最可能的端点，并在代码中将其封装为**集中可配置常量**，例如：
   - `const CHAT_API_URL = "/api/chat_stream"; // TODO: 若不符，请改成实际后端地址`

### 2. Design Data Structures (Front-End)

基于后端结构，设计前端使用的 TypeScript 接口，例如：

- `ChatMessage`：单条消息
  - 必须包含：
    - `id: string`
    - `role: "user" | "assistant"`（或与后端对齐的角色字符串）
    - `content: string`
    - 可选字段：`createdAt`, `status`, `error`, `raw` 等。
- `Conversation`：会话
  - `id: string`
  - `title: string`
  - `messages: ChatMessage[]`
  - `createdAt: string | number | Date`
  - `isActive: boolean`

确保字段命名 **优先与后端 DTO 对齐**，其次才考虑前端偏好。

### 3. Implement the Vue 3 Component

在生成 `.vue` 文件时遵循：

- 使用 `<script setup>` + Composition API：
  - `ref`, `computed`, `onMounted`, `watch`, `nextTick` 等。
- 网络请求：
  - 使用原生 `fetch`；如项目已有 axios 等，可根据项目情况采用，但要确保**无需额外安装依赖**为佳。
- 状态管理：在组件内本地管理（不引入全局 store），除非项目现有明显的 store 方案并且用户显式要求。

#### Template 要求

模板结构一般如下（可按需调整）：

- 外层：全屏容器，背景纯黑 + 毛玻璃层。
- 左侧栏（会话列表）：
  - 顶部：Logo/标题（极简文本，如 “Agent”）、“新建对话”按钮。
  - 中部：会话列表，当前选中高亮。
  - 底部：可选的版本/状态信息。
- 主区域：
  - 顶部导航栏：
    - 左：当前会话标题。
    - 中：会话 ID（短 ID）或后端状态。
    - 右：连接状态点（如 circle + “Connected”/“Idle” 文本）。
  - 消息列表：
    - 用户消息：右对齐、深色实心气泡。
    - AI 消息：左对齐、毛玻璃卡片、带淡边框。
    - 自动滚动到底部。
  - 底部输入区：
    - 文本输入框 + 辅助按钮（可选）。
    - 发送按钮；回车发送，Shift+Enter 换行（推荐）。
    - 加载中时按钮禁用，显示 spinner 或 “思考中...” 文案。

#### Script 逻辑要点

- 维护状态：
  - `const conversations = ref<Conversation[]>([])`
  - `const activeConversationId = ref<string | null>(null)`
  - `const messages = computed(() => 当前会话的 messages)`
  - `const input = ref("")`
  - `const isLoading = ref(false)`
  - `const error = ref<string | null>(null)`
- 关键方法：
  - `createConversation()`
  - `switchConversation(id: string)`
  - `sendMessage()`
    - 无内容时 return。
    - 添加本地“用户消息”到列表。
    - 调用后端接口，处理响应，推入 AI 消息。
    - catch 时设置 `error` 并清理 loading。
  - `scrollToBottom()` 使用 `nextTick` + `scrollIntoView` 或 `element.scrollTop = element.scrollHeight`。
- 生命周期：
  - `onMounted` 时，可选读取本地存储历史会话。
  - `watch(activeConversationId, ...)` 在切换时滚动到最底。

---

## UI Styling Rules (Tailwind-Centric)

在 `<style scoped>` 中可以补充自定义样式，但尽量依赖 Tailwind 类名。

- 常见类推荐：
  - 布局：`flex`, `flex-col`, `flex-1`, `min-h-screen`, `max-w-...`, `mx-auto`, `overflow-hidden`, `overflow-y-auto`
  - 背景：`bg-black`, `bg-neutral-950`, `bg-gradient-to-b from-black via-black to-neutral-950`（可选但需克制）、`bg-white/5`, `bg-white/2`, `bg-neutral-900/60`
  - 毛玻璃：`backdrop-blur-xl`, `backdrop-saturate-150`
  - 边框：`border`, `border-white/5`, `border-neutral-800`
  - 阴影：`shadow-lg`, `shadow-2xl`, `shadow-black/60`
  - 文本：`text-white`, `text-neutral-300`, `text-neutral-500`, `text-xs`, `text-sm`, `text-base`
  - 圆角：`rounded-lg`, `rounded-2xl`, `rounded-full`
  - 动效：`transition`, `duration-200`, `ease-out`, `hover:bg-white/10`, `hover:-translate-y-0.5`
- 禁止：
  - 高饱和度背景色（如 `bg-blue-500`）大面积使用。
  - 华丽渐变与复杂 animation keyframes。

---

## Error Handling and Empty States

- **错误展示**：
  - 在主内容顶部或底部显示一条带 `bg-red-500/10 border-red-500/40 text-red-300 text-xs` 的小条。
  - 包含简要错误描述，可提供“重试”按钮。
- **空状态**：
  - 对话区在无消息时显示：
    - 简洁图标（使用纯 CSS/文字，如一个 `AI` 标记）。
    - 说明文案，例如：“开始一段新的对话，让 Agent 帮你连接后端服务。”

---

## Output Requirements

当用户请求使用本技能生成 UI 时，最终回答应：

1. **只输出完整的 Vue 单文件组件代码**（在普通聊天中至少应有一个专门消息只包含代码块）。
2. 确保代码：
   - 语法完整可编译。
   - 所有引用的标识符（变量、函数、常量）均已定义在文件内或为明显的外部依赖（如全局 Tailwind）。
3. 不添加与 UI 无关的额外说明文本，除非用户显式要求解释。

---

## Example Usage Pattern

（逻辑示例，实际生成时应直接给出 `.vue` 文件，无需包含本节注释）

- 用户说：
  - “帮我为这个 Spring Boot + AI Agent 后端生成一个纯黑毛玻璃的 Vue3 AI 对话界面，能直接调 `/api/chat_stream` 接口。”
- 代理行为：
  1. 根据本技能指引，检查项目中与 `/api/chat_stream` 相关的接口定义，推断请求/响应结构。
  2. 设计 `ChatMessage`、`Conversation` 等前端类型，字段名对齐后端 DTO。
  3. 生成一个 `AgentChat.vue` 单文件组件，实现：
     - 左侧会话列表 + 顶部导航 + 中间聊天气泡 + 底部输入。
     - 输入框回车发送 / 按钮发送。
     - 加载状态、错误提示、空状态。
     - 与 `/api/chat_stream` 对接的异步请求逻辑（流式或非流式）。
  4. 将组件完整代码返回给用户，可直接复制使用。

---
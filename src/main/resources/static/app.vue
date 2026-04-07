<template>
  <div class="min-h-screen bg-black text-white font-sans">
    <!-- 主容器 -->
    <div class="flex h-screen overflow-hidden">
      <!-- 左侧会话列表 -->
      <div class="w-80 border-r border-dark-gray-4 bg-dark-gray-1 flex flex-col">
        <!-- 新建会话按钮 -->
        <div class="p-4 border-b border-dark-gray-4">
          <button 
            @click="createNewSession"
            class="w-full h-12 bg-dark-gray-3 border border-dark-gray-4 rounded-lg hover:bg-dark-gray-4 transition-all flex items-center justify-center gap-2"
          >
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5">
              <path d="M12 5V19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>新建会话</span>
          </button>
        </div>
        
        <!-- 会话列表 -->
        <div class="flex-1 overflow-y-auto">
          <div 
            v-for="session in sessions" 
            :key="session.id"
            @click="switchSession(session.id)"
            class="px-4 py-3 border-b border-dark-gray-4 hover:bg-dark-gray-3 cursor-pointer transition-colors"
            :class="{ 'bg-dark-gray-3': currentSessionId === session.id }"
          >
            <div class="font-medium truncate">{{ session.title }}</div>
            <div class="text-xs text-white/50 truncate mt-1">{{ session.lastMessage || '无消息' }}</div>
            <div class="flex justify-between items-center mt-2">
              <span class="text-xs text-white/30">{{ formatTime(session.timestamp) }}</span>
              <button 
                @click.stop="deleteSession(session.id)"
                class="w-6 h-6 flex items-center justify-center rounded hover:bg-white/10 transition-colors"
              >
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-4 h-4">
                  <path d="M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧聊天区域 -->
      <div class="flex-1 flex flex-col">
        <!-- 顶部状态栏 -->
        <div class="h-16 border-b border-dark-gray-4 bg-dark-gray-1 flex items-center justify-between px-6">
          <div>
            <div class="font-semibold">{{ currentSession?.title || '新会话' }}</div>
            <div class="text-xs text-white/50">会话 ID: {{ currentSessionId }}</div>
          </div>
          <div class="flex items-center gap-3">
            <!-- 模式选择按钮 -->
            <div class="relative">
              <button 
                @click="toggleModeDropdown"
                :disabled="isLoading"
                class="px-3 h-10 bg-dark-gray-3 border border-dark-gray-4 rounded-lg flex items-center gap-2 hover:bg-dark-gray-4 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span class="text-sm">{{ modeNames[currentMode] }}</span>
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-white/60 transition-transform" :class="{'rotate-180': showModeDropdown}">
                  <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <div 
                v-if="showModeDropdown"
                class="absolute bottom-full right-0 mb-2 bg-dark-gray-2 border border-dark-gray-4 rounded-lg shadow-xl overflow-hidden"
              >
                <div 
                  v-for="mode in modes" 
                  :key="mode.value"
                  @click="selectMode(mode.value)"
                  class="px-4 py-3 hover:bg-dark-gray-3 cursor-pointer transition-colors"
                  :class="{'bg-dark-gray-3': currentMode === mode.value}"
                >
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium">{{ mode.label }}</span>
                    <span v-if="mode.badge" class="text-xs bg-white/20 px-2 py-0.5 rounded">{{ mode.badge }}</span>
                  </div>
                  <div class="text-xs text-white/50 mt-1">{{ mode.description }}</div>
                </div>
              </div>
            </div>
            
            <!-- AI Ops 按钮 -->
            <button 
              @click="triggerAIOps"
              :disabled="isLoading"
              class="px-4 h-10 bg-blue-500 hover:bg-blue-600 transition-all rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
            >
              AI Ops
            </button>
          </div>
        </div>
        
        <!-- 消息区域 -->
        <div 
          ref="messageContainer"
          class="flex-1 overflow-y-auto p-6 space-y-6"
          @scroll="handleScroll"
        >
          <div 
            v-for="(message, index) in currentSession?.messages || []" 
            :key="index"
            class="flex"
            :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div 
              class="max-w-[80%] p-4 rounded-lg"
              :class="message.role === 'user' ? 'bg-blue-500 text-white' : 'bg-dark-gray-3 text-white'"
            >
              <div v-if="message.loading" class="flex items-center gap-2">
                <div class="w-2 h-2 bg-white/60 rounded-full animate-pulse"></div>
                <div class="w-2 h-2 bg-white/60 rounded-full animate-pulse" style="animation-delay: 0.2s"></div>
                <div class="w-2 h-2 bg-white/60 rounded-full animate-pulse" style="animation-delay: 0.4s"></div>
              </div>
              <div v-else v-html="renderMarkdown(message.content)"></div>
              <div class="text-xs text-white/50 mt-2 text-right">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="border-t border-dark-gray-4 bg-dark-gray-1 p-4">
          <div class="flex items-center gap-3">
            <!-- 文件上传按钮 -->
            <button 
              @click="triggerFileUpload"
              :disabled="isLoading"
              class="w-10 h-10 bg-dark-gray-3 border border-dark-gray-4 rounded-lg flex items-center justify-center hover:bg-dark-gray-4 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5">
                <path d="M12 21V15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M9 12L12 15L15 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M21 15V8C21 6.34315 19.6569 5 18 5H6C4.34315 5 3 6.34315 3 8V15C3 16.6569 4.34315 18 6 18H18C19.6569 18 21 16.6569 21 15Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <input type="file" ref="fileInput" @change="handleFileUpload" accept=".txt,.md,.markdown" class="hidden" />
            
            <!-- 输入框 -->
            <div class="flex-1 relative">
              <textarea 
                v-model="inputMessage"
                @keydown.enter.exact="sendMessage"
                @keydown.enter.shift.prevent=""
                :disabled="isLoading"
                placeholder="输入消息..."
                class="w-full h-14 p-4 bg-dark-gray-3 border border-dark-gray-4 rounded-lg resize-none focus:outline-none focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              ></textarea>
            </div>
            
            <!-- 发送按钮 -->
            <button 
              @click="sendMessage"
              :disabled="!inputMessage.trim() || isLoading"
              class="w-14 h-14 bg-blue-500 hover:bg-blue-600 transition-all rounded-lg flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-6 h-6">
                <path d="M22 2L11 13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载遮罩层 -->
    <div v-if="showLoadingOverlay" class="fixed inset-0 bg-black/80 backdrop-blur-xl flex items-center justify-center z-50">
      <div class="bg-dark-gray-2 border border-dark-gray-4 rounded-xl p-8 text-center max-w-md w-full">
        <div class="w-16 h-16 border-4 border-white/20 border-t-white/60 rounded-full animate-spin mx-auto mb-6"></div>
        <div class="text-lg font-semibold mb-4">智能运维分析中，请稍候...</div>
        <div class="text-sm text-white/60 mb-6">后端正在处理，请耐心等待</div>
        <!-- 模拟进度条 -->
        <div class="w-full h-2 bg-dark-gray-3 rounded-full overflow-hidden">
          <div 
            class="h-full bg-blue-500 transition-all duration-300 ease-in-out"
            :style="{ width: loadingProgress + '%' }"
          ></div>
        </div>
        <div class="text-xs text-white/50 mt-2">{{ Math.round(loadingProgress) }}%</div>
      </div>
    </div>
    
    <!-- 通知提示 -->
    <div 
      v-if="notification.show"
      class="fixed top-4 right-4 px-6 py-3 rounded-lg shadow-xl z-50 transition-all transform translate-y-0"
      :class="{
        'bg-green-500': notification.type === 'success',
        'bg-red-500': notification.type === 'error',
        'bg-yellow-500': notification.type === 'warning',
        'bg-blue-500': notification.type === 'info'
      }"
    >
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { marked } from 'https://cdn.jsdelivr.net/npm/marked@14.0.0/+esm';
import hljs from 'https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/+esm';

// API 基础路径
const apiBaseUrl = 'http://localhost:9900/api';

// 响应式状态
const currentSessionId = ref('');
const sessions = ref([]);
const isLoading = ref(false);
const isAtBottom = ref(true);
const currentMode = ref('quick');
const showModeDropdown = ref(false);
const showLoadingOverlay = ref(false);
const loadingProgress = ref(0);
const loadingInterval = ref(null);
const notification = ref({ show: false, message: '', type: 'info' });
const inputMessage = ref('');
const messageContainer = ref(null);
const fileInput = ref(null);

// 模式配置
const modes = [
  { value: 'quick', label: '快速', description: '快速对话', badge: '新' },
  { value: 'stream', label: '流式', description: '流式对话' },
  { value: 'summary', label: '总结', description: '总结对话' }
];

const modeNames = {
  'quick': '快速',
  'stream': '流式',
  'summary': '总结'
};

// 计算当前会话
const currentSession = computed(() => {
  return sessions.value.find(session => session.id === currentSessionId.value) || null;
});

// 初始化
onMounted(() => {
  loadSessions();
  if (sessions.value.length === 0) {
    createNewSession();
  } else {
    currentSessionId.value = sessions.value[0].id;
  }
});

// 加载会话
const loadSessions = () => {
  const savedSessions = localStorage.getItem('chat_sessions');
  if (savedSessions) {
    sessions.value = JSON.parse(savedSessions);
  }
};

// 保存会话
const saveSessions = () => {
  localStorage.setItem('chat_sessions', JSON.stringify(sessions.value));
};

// 创建新会话
const createNewSession = () => {
  const newSession = {
    id: Date.now().toString(),
    title: '新会话',
    messages: [],
    lastMessage: '',
    timestamp: Date.now(),
    mode: currentMode.value
  };
  sessions.value.unshift(newSession);
  currentSessionId.value = newSession.id;
  saveSessions();
};

// 切换会话
const switchSession = (sessionId) => {
  currentSessionId.value = sessionId;
  const session = sessions.value.find(s => s.id === sessionId);
  if (session) {
    currentMode.value = session.mode;
  }
  scrollToBottom();
};

// 删除会话
const deleteSession = (sessionId) => {
  const index = sessions.value.findIndex(s => s.id === sessionId);
  if (index !== -1) {
    sessions.value.splice(index, 1);
    saveSessions();
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = sessions.value.length > 0 ? sessions.value[0].id : '';
      if (sessions.value.length > 0) {
        currentMode.value = sessions.value[0].mode;
      }
    }
  }
};

// 选择模式
const selectMode = (mode) => {
  currentMode.value = mode;
  showModeDropdown.value = false;
  if (currentSession.value) {
    currentSession.value.mode = mode;
    saveSessions();
  }
};

// 切换模式下拉框
const toggleModeDropdown = () => {
  showModeDropdown.value = !showModeDropdown.value;
};

// 发送消息
const sendMessage = async () => {
  const message = inputMessage.value.trim();
  if (!message || isLoading.value) return;

  // 添加用户消息
  const userMessage = {
    role: 'user',
    content: message,
    timestamp: Date.now()
  };
  
  currentSession.value.messages.push(userMessage);
  currentSession.value.lastMessage = message;
  currentSession.value.title = message.length > 20 ? message.substring(0, 20) + '...' : message;
  inputMessage.value = '';
  saveSessions();
  scrollToBottom();

  // 显示加载状态
  const loadingMessage = {
    role: 'assistant',
    content: '',
    loading: true,
    timestamp: Date.now()
  };
  currentSession.value.messages.push(loadingMessage);
  scrollToBottom();

  isLoading.value = true;

  try {
    if (currentMode.value === 'quick') {
      await sendQuickMessage(message, loadingMessage);
    } else if (currentMode.value === 'stream') {
      await sendStreamMessage(message, loadingMessage);
    } else if (currentMode.value === 'summary') {
      await sendSummaryMessage(message, loadingMessage);
    }
  } catch (error) {
    loadingMessage.content = `错误: ${error.message}`;
    loadingMessage.loading = false;
    showNotification('发送消息失败: ' + error.message, 'error');
  } finally {
    isLoading.value = false;
    updateCurrentSession();
    scrollToBottom();
  }
};

// 发送快速消息
const sendQuickMessage = async (message, loadingMessage) => {
  const response = await fetch(`${apiBaseUrl}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      Id: currentSessionId.value,
      Question: message
    })
  });

  const data = await response.json();
  
  if (data.code === 200 && data.data.success) {
    loadingMessage.content = data.data.answer;
    loadingMessage.loading = false;
    currentSession.value.lastMessage = data.data.answer;
    saveSessions();
  } else {
    throw new Error(data.data.errorMessage || '未知错误');
  }
};

// 发送流式消息
const sendStreamMessage = async (message, loadingMessage) => {
  const response = await fetch(`${apiBaseUrl}/chat_stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      Id: currentSessionId.value,
      Question: message
    })
  });

  if (!response.ok) {
    throw new Error(`HTTP错误: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let fullResponse = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      
      for (const line of lines) {
        if (line.trim() === '') continue;
        
        if (line.startsWith('data:')) {
          const rawData = line.substring(5).trim();
          
          try {
            const sseMessage = JSON.parse(rawData);
            
            if (sseMessage.type === 'content') {
              fullResponse += sseMessage.data || '';
              loadingMessage.content = fullResponse;
              await nextTick();
              scrollToBottom();
            } else if (sseMessage.type === 'done') {
              loadingMessage.loading = false;
              currentSession.value.lastMessage = fullResponse;
              saveSessions();
              return;
            } else if (sseMessage.type === 'error') {
              throw new Error(sseMessage.data || '未知错误');
            }
          } catch (e) {
            console.error('解析SSE消息失败:', e);
            // 尝试直接将内容添加到响应中，以处理非JSON格式的SSE
            if (rawData !== '[DONE]') {
              fullResponse += rawData;
              loadingMessage.content = fullResponse;
              await nextTick();
              scrollToBottom();
            }
          }
        } else {
          // 处理非标准SSE格式的响应
          fullResponse += line;
          loadingMessage.content = fullResponse;
          await nextTick();
          scrollToBottom();
        }
      }
    }
  } finally {
    reader.releaseLock();
    loadingMessage.loading = false;
    // 确保消息状态被更新
    if (fullResponse) {
      currentSession.value.lastMessage = fullResponse;
      saveSessions();
    }
  }
};

// 发送总结消息
const sendSummaryMessage = async (message, loadingMessage) => {
  const response = await fetch(`${apiBaseUrl}/chat_summary`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      Id: currentSessionId.value,
      Question: message
    })
  });

  const data = await response.json();
  
  if (data.code === 200 && data.data.success) {
    loadingMessage.content = data.data.answer;
    loadingMessage.loading = false;
    currentSession.value.lastMessage = data.data.answer;
    saveSessions();
  } else {
    throw new Error(data.data.errorMessage || '未知错误');
  }
};

// 触发AI Ops
const triggerAIOps = async () => {
  if (isLoading.value) {
    showNotification('请等待当前对话完成', 'warning');
    return;
  }

  showLoadingOverlay.value = true;
  loadingProgress.value = 0;
  isLoading.value = true;
  
  // 启动模拟进度条
  if (loadingInterval.value) {
    clearInterval(loadingInterval.value);
  }
  loadingInterval.value = setInterval(() => {
    if (loadingProgress.value < 100) {
      loadingProgress.value += Math.random() * 5 + 1;
      if (loadingProgress.value > 100) {
        loadingProgress.value = 100;
      }
    }
  }, 200);

  try {
    const response = await fetch(`${apiBaseUrl}/ai_ops`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`HTTP错误: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let fullResponse = '';

    // 添加AI Ops消息
    const aiOpsMessage = {
      role: 'assistant',
      content: '',
      loading: true,
      timestamp: Date.now()
    };
    currentSession.value.messages.push(aiOpsMessage);
    scrollToBottom();

    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          if (line.trim() === '') continue;
          
          if (line.startsWith('data:')) {
            const rawData = line.substring(5).trim();
            
            try {
              const sseMessage = JSON.parse(rawData);
              
              if (sseMessage.type === 'content') {
                fullResponse += sseMessage.data || '';
                aiOpsMessage.content = fullResponse;
                await nextTick();
                scrollToBottom();
              } else if (sseMessage.type === 'done') {
                aiOpsMessage.loading = false;
                currentSession.value.lastMessage = 'AI Ops 分析完成';
                saveSessions();
                return;
              } else if (sseMessage.type === 'error') {
                throw new Error(sseMessage.data || '未知错误');
              }
            } catch (e) {
              console.error('解析SSE消息失败:', e);
            }
          }
        }
      }
    } finally {
      reader.releaseLock();
      aiOpsMessage.loading = false;
    }

    showNotification('AI Ops 分析完成', 'success');
  } catch (error) {
    showNotification('AI Ops 执行失败: ' + error.message, 'error');
  } finally {
    // 清除进度条定时器
    if (loadingInterval.value) {
      clearInterval(loadingInterval.value);
      loadingInterval.value = null;
    }
    loadingProgress.value = 100;
    // 延迟关闭遮罩层，让用户看到100%的进度
    setTimeout(() => {
      showLoadingOverlay.value = false;
      isLoading.value = false;
    }, 500);
  }
};

// 文件上传
const triggerFileUpload = () => {
  fileInput.value.click();
};

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await fetch(`${apiBaseUrl}/upload`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP错误: ${response.status}`);
    }

    const data = await response.json();
    showNotification('文件上传成功', 'success');
  } catch (error) {
    showNotification('文件上传失败: ' + error.message, 'error');
  }
};

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
  }
};

// 处理滚动
const handleScroll = () => {
  if (messageContainer.value) {
    const { scrollTop, scrollHeight, clientHeight } = messageContainer.value;
    isAtBottom.value = scrollTop + clientHeight >= scrollHeight - 10;
  }
};

// 更新当前会话
const updateCurrentSession = () => {
  const index = sessions.value.findIndex(s => s.id === currentSessionId.value);
  if (index !== -1) {
    sessions.value[index] = { ...currentSession.value };
    saveSessions();
  }
};

// 显示通知
const showNotification = (message, type = 'info') => {
  notification.value = {
    show: true,
    message,
    type
  };
  
  setTimeout(() => {
    notification.value.show = false;
  }, 3000);
};

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return '';
  
  // 配置marked
  marked.setOptions({
    highlight: function(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value;
      }
      return hljs.highlightAuto(code).value;
    },
    breaks: true,
    gfm: true
  });
  
  return marked(content);
};
</script>

<style scoped>
/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #1a1a1a;
}

::-webkit-scrollbar-thumb {
  background: #333;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #444;
}

/* Markdown样式 */
:deep(.prose) {
  max-width: 100%;
  color: #fff;
}

:deep(.prose code) {
  background-color: rgba(255, 255, 255, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 0.9em;
}

:deep(.prose pre) {
  background-color: rgba(255, 255, 255, 0.05);
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
}

:deep(.prose pre code) {
  background: none;
  padding: 0;
}

:deep(.prose blockquote) {
  border-left: 4px solid #333;
  padding-left: 1em;
  color: rgba(255, 255, 255, 0.7);
}
</style>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: #000;
  color: #fff;
  line-height: 1.6;
}

/* 深色主题变量 */
:root {
  --dark-gray-1: #111;
  --dark-gray-2: #1a1a1a;
  --dark-gray-3: #222;
  --dark-gray-4: #333;
}

.bg-black {
  background-color: #000;
}

.bg-dark-gray-1 {
  background-color: var(--dark-gray-1);
}

.bg-dark-gray-2 {
  background-color: var(--dark-gray-2);
}

.bg-dark-gray-3 {
  background-color: var(--dark-gray-3);
}

.bg-dark-gray-4 {
  background-color: var(--dark-gray-4);
}

.border-dark-gray-4 {
  border-color: var(--dark-gray-4);
}

.text-white {
  color: #fff;
}

.animate-pulse {
  animation: pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.rotate-180 {
  transform: rotate(180deg);
}

.transition-all {
  transition: all 0.3s ease;
}

.transition-colors {
  transition: color 0.3s ease, background-color 0.3s ease;
}

.transition-transform {
  transition: transform 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .w-80 {
    width: 200px;
  }
}
</style>
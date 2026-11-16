<template>
  <div class="chat-page">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <div class="chat-title">
        <span class="chat-logo">🤖</span>
        <div class="chat-title-text">
          <h2>与 AI 健康助手对话</h2>
          <p>基于智谱 GLM 免费模型 · 对话记录自动保存</p>
        </div>
      </div>
      <el-button
        size="small"
        type="danger"
        plain
        :disabled="!messages.length"
        @click="clearChat"
      >清空对话</el-button>
    </div>

    <!-- 消息区 -->
    <div class="chat-body" ref="bodyRef">
      <el-empty
        v-if="!messages.length && !loading"
        description="还没有对话，发送第一条消息开始吧～"
        :image-size="90"
      />

      <div
        v-for="(m, i) in messages"
        :key="i"
        class="msg-row"
        :class="m.role"
      >
        <div class="avatar">{{ m.role === 'user' ? '🧑' : '🤖' }}</div>
        <div class="bubble" v-html="formatMsg(m.content)"></div>
      </div>

      <!-- AI 输入中动画 -->
      <div v-if="loading" class="msg-row assistant">
        <div class="avatar">🤖</div>
        <div class="bubble typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        resize="none"
        maxlength="2000"
        show-word-limit
        placeholder="输入你的问题，例如：我最近总是失眠，有什么建议？"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const bodyRef = ref(null)

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

// 加载历史对话
const loadHistory = async () => {
  try {
    const res = await request.post('/ai/chat-history')
    messages.value = (res || []).map((m) => ({ role: m.role, content: m.content }))
    scrollToBottom()
  } catch (e) {
    console.error('加载对话历史失败:', e)
  }
}

// 发送消息
const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // AI 生成回复耗时较长，单独放宽超时到 60s（全局默认 5s 会超时）
    const res = await request.post('/ai/chat', { message: text }, { timeout: 60000 })
    messages.value.push({ role: 'assistant', content: res.reply })
  } catch (e) {
    console.error('AI 回复失败:', e)
    ElMessage.error('AI 回复失败，请稍后重试')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 清空对话
const clearChat = () => {
  ElMessageBox.confirm('确定清空所有对话记录吗？此操作不可恢复。', '提示', { type: 'warning' })
    .then(async () => {
      await request.post('/ai/chat-clear')
      messages.value = []
      ElMessage.success('已清空对话记录')
    })
    .catch(() => {})
}

// 安全地格式化消息（先转义 HTML，再渲染换行/加粗/代码）
const formatMsg = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+?)`/g, '<code>$1</code>')
}

onMounted(loadHistory)
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 114px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(159, 29, 29, 0.08);
  overflow: hidden;
}

/* 顶部栏 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0e4e4;
  background: linear-gradient(90deg, #fff 0%, #fdf3f3 100%);
}
.chat-title { display: flex; align-items: center; gap: 12px; }
.chat-logo {
  font-size: 30px;
  width: 46px; height: 46px;
  display: flex; align-items: center; justify-content: center;
  background: #fdeaea; border-radius: 12px;
}
.chat-title-text h2 { margin: 0; font-size: 18px; color: #7f1010; }
.chat-title-text p { margin: 2px 0 0; font-size: 12px; color: #909399; }

/* 消息区 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #faf7f7;
}
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}
.msg-row.user { flex-direction: row-reverse; }
.avatar {
  width: 38px; height: 38px;
  flex: 0 0 38px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 50%;
}
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.7;
  font-size: 14px;
  white-space: normal;
  word-break: break-word;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.msg-row.assistant .bubble {
  background: #fff;
  color: #303133;
  border-top-left-radius: 2px;
}
.msg-row.user .bubble {
  background: #7f1010;
  color: #fff;
  border-top-right-radius: 2px;
}
.bubble :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}
.msg-row.user .bubble :deep(code) { background: rgba(255,255,255,0.2); }

/* 输入中三点动画 */
.bubble.typing { display: flex; gap: 4px; align-items: center; padding: 14px; }
.bubble.typing span {
  width: 7px; height: 7px; border-radius: 50%;
  background: #c0a0a0;
  animation: blink 1.2s infinite both;
}
.bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

/* 输入区 */
.chat-input {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #f0e4e4;
  background: #fff;
  align-items: flex-end;
}
.chat-input .el-input { flex: 1; }
.chat-input .el-button { height: 40px; padding: 0 26px; }
</style>

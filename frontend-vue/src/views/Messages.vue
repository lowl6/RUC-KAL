<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { messagesApi } from '@/api/projects'
import { normalizeConversation, normalizeMessage } from '@/api/normalize'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const user = useUserStore()

const conversations = ref([])
const activeId = ref('')
const search = ref('')
const messageInput = ref('')
const messages = ref([])
const loading = ref(false)
const error = ref('')
const sending = ref(false)

const filteredConv = computed(() => {
  if (!search.value) return conversations.value
  const k = search.value.toLowerCase()
  return conversations.value.filter(c =>
    c.counterpart.name.toLowerCase().includes(k) ||
    c.last_message.toLowerCase().includes(k)
  )
})
const activeConv = computed(() => conversations.value.find(c => c.conversation_id === activeId.value))

async function selectConv(c) {
  activeId.value = c.conversation_id
  c.unread_count = 0
  messages.value = []
  await loadHistory(c.conversation_id)
  try { await messagesApi.markRead(c.conversation_id) } catch (e) {}
  user.refreshUnread()
}

async function loadConversations() {
  if (!auth.isLoggedIn) {
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  loading.value = true
  error.value = ''
  try {
    if (route.query.userId) {
      const opened = await messagesApi.open(route.query.userId)
      activeId.value = opened.conversationId
      router.replace({ path: '/messages', query: { conversation: opened.conversationId } })
    }
    const raw = await messagesApi.conversations()
    conversations.value = (raw || []).map(c => normalizeConversation(c, auth.me?.userId))
    if (route.query.conversation) activeId.value = route.query.conversation
    if (!activeId.value && conversations.value.length) activeId.value = conversations.value[0].conversation_id
    if (activeId.value) {
      await loadHistory(activeId.value)
      try { await messagesApi.markRead(activeId.value) } catch (e) {}
      user.refreshUnread()
    } else {
      messages.value = []
    }
  } catch (e) {
    error.value = '会话同步失败：' + (e.message || '后端不可用')
    conversations.value = []
    messages.value = []
    activeId.value = ''
  } finally {
    loading.value = false
  }
}

async function loadHistory(id) {
  if (!id) { messages.value = []; return }
  try {
    const raw = await messagesApi.history(id)
    messages.value = (raw || []).map(m => normalizeMessage(m, auth.me?.userId))
    await scrollToBottom()
  } catch (e) {
    messages.value = []
    error.value = '消息加载失败：' + (e.message || '请稍后重试')
  }
}

async function sendMessage() {
  const text = messageInput.value.trim()
  if (!text || !activeId.value) return
  sending.value = true
  try {
    const saved = await messagesApi.send(activeId.value, text)
    messages.value.push(normalizeMessage(saved, auth.me?.userId))
    messageInput.value = ''
    await loadConversations()
    activeId.value = saved.conversationId || activeId.value
  } catch (e) {
    error.value = e.message || '发送失败，请确认后端已启动。'
  } finally {
    sending.value = false
  }
  await scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    const list = document.querySelector('.km-msgs')
    if (list) list.scrollTop = list.scrollHeight
  })
}

function quickAction(type) {
  const presets = {
    wechat: '我的微信号是：liu_yifan_2022',
    skills: '我的技能：Python、机器学习、数据分析、TensorFlow',
    project: '我们的项目：智慧校园 AI 学习助手 → /projects/p_001'
  }
  messageInput.value = presets[type] || ''
}

watch(() => route.query.conversation, (id) => {
  if (id && id !== activeId.value) {
    activeId.value = id
    loadHistory(id)
  }
})

onMounted(loadConversations)
</script>

<template>
  <div class="kal-container km-wrap">
    <header class="km-head">
      <div class="kal-eyebrow">Direct&nbsp;Messages</div>
      <h1 class="km-h1">私信</h1>
    </header>

    <div v-if="error" class="kal-card km-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card km-notice">正在同步会话…</div>

    <div class="km">
      <!-- 会话列表 -->
      <aside class="km-list">
        <header class="km-list-head">
          <span class="km-list-title">对话列表</span>
          <span class="km-list-count">{{ filteredConv.length }}</span>
        </header>
        <div class="km-search">
          <Icon name="search" :size="13" />
          <input v-model="search" placeholder="搜索昵称或消息" />
        </div>
        <div class="km-conv-list">
          <button
            v-for="c in filteredConv"
            :key="c.conversation_id"
            class="km-conv"
            :class="{ 'km-conv--active': c.conversation_id === activeId }"
            @click="selectConv(c)"
          >
            <span class="kal-avatar">{{ c.counterpart.initial }}</span>
            <div class="km-conv-body">
              <div class="km-conv-row1">
                <span class="km-conv-name">{{ c.counterpart.name }}</span>
                <span class="km-conv-time">{{ c.last_time }}</span>
              </div>
              <div class="km-conv-preview">{{ c.last_message }}</div>
              <div class="km-conv-context">
                <span class="km-conv-tag">{{ c.context_label }}</span>
                <span v-if="c.is_expired" class="km-conv-expired">已过期</span>
              </div>
            </div>
            <span v-if="c.unread_count" class="km-unread">{{ c.unread_count }}</span>
          </button>
        </div>
      </aside>

      <!-- 聊天面板 -->
      <section class="km-chat" v-if="activeConv">
        <header class="km-chat-head">
          <span class="kal-avatar">{{ activeConv.counterpart.initial }}</span>
          <div>
            <div class="km-chat-name">{{ activeConv.counterpart.name }}</div>
            <div class="km-chat-context">{{ activeConv.context_label }}</div>
          </div>
          <button class="kh-icon-action" title="查看资料">
            <Icon name="info" :size="16" />
          </button>
        </header>

        <div v-if="activeConv.is_expired" class="km-banner">
          <Icon name="info" :size="13" />
          <span>此对话超过 7 日未活跃，已归档（仅供查阅，无法续写）。</span>
        </div>

        <div class="km-msgs">
          <div class="km-system">
            <span class="km-rule"></span>
            <span>对话开始 · 平台不会保存交换微信后的私聊</span>
            <span class="km-rule"></span>
          </div>
          <div
            v-for="m in messages"
            :key="m.id"
            class="km-msg"
            :class="`km-msg--${m.from}`"
          >
            <span v-if="m.from === 'them'" class="kal-avatar kal-avatar-sm">{{ activeConv.counterpart.initial }}</span>
            <div class="km-bubble-wrap">
              <div class="km-bubble">{{ m.text }}</div>
              <div class="km-msg-time">{{ m.time }}</div>
            </div>
          </div>
        </div>

        <footer class="km-input">
          <div class="km-quicks">
            <button class="km-quick" @click="quickAction('wechat')">
              <Icon name="user" :size="12" />
              <span>发送微信号</span>
            </button>
            <button class="km-quick" @click="quickAction('skills')">
              <Icon name="target" :size="12" />
              <span>发送技能</span>
            </button>
            <button class="km-quick" @click="quickAction('project')">
              <Icon name="briefcase" :size="12" />
              <span>发送项目</span>
            </button>
          </div>
          <div class="km-input-row">
            <textarea
              class="kal-textarea km-textarea"
              v-model="messageInput"
              placeholder="输入消息，Enter 发送 / Shift+Enter 换行"
              :disabled="activeConv.is_expired || sending"
              @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <button class="kal-btn km-send" :disabled="!messageInput.trim() || activeConv.is_expired || sending" @click="sendMessage">
              <span>{{ sending ? '发送中…' : '发送' }}</span>
              <Icon name="send" :size="13" />
            </button>
          </div>
        </footer>
      </section>

      <section v-else class="km-empty kal-empty">
        <div class="kal-empty-title">暂无会话</div>
        <div class="kal-empty-desc">从项目卡或个人卡进入私信，会自动创建一段对话。</div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.km-wrap { max-width: 1200px; }
.km-head { margin-bottom: 24px; }
.km-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 12px; }
.km-h1 {
  font-family: var(--kal-font-serif);
  font-size: 36px;
  font-weight: 600;
  letter-spacing: 5px;
  color: var(--kal-text-strong);
}
.km-notice {
  padding: 12px 16px;
  margin-bottom: 16px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
}

.km {
  display: grid;
  grid-template-columns: 320px 1fr;
  height: calc(100vh - var(--kal-header-height) - 64px - 160px);
  min-height: 580px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  overflow: hidden;
}

/* ---------- List ---------- */
.km-list {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--kal-divider);
  background: var(--kal-bg-subtle);
}
.km-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 22px 14px;
}
.km-list-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-base);
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--kal-text-strong);
}
.km-list-count {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-subtle);
}
.km-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  margin: 0 16px 8px;
  height: 34px;
  background: #fff;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  color: var(--kal-text-subtle);
}
.km-search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: var(--kal-text-sm);
  color: var(--kal-text);
}
.km-conv-list { flex: 1; overflow-y: auto; padding: 6px 0; }
.km-conv {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 14px 22px;
  background: transparent;
  border: none;
  text-align: left;
  cursor: pointer;
  position: relative;
  transition: background var(--kal-duration-2);
}
.km-conv:hover { background: rgba(255, 255, 255, 0.5); }
.km-conv--active { background: #fff; box-shadow: inset 2px 0 0 var(--kal-primary-700); }
.km-conv-body { flex: 1; min-width: 0; }
.km-conv-row1 { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 4px; }
.km-conv-name { font-weight: 600; font-size: var(--kal-text-md); color: var(--kal-text-strong); letter-spacing: 0.5px; }
.km-conv-time { font-size: 11px; color: var(--kal-text-subtle); flex-shrink: 0; letter-spacing: 0.5px; }
.km-conv-preview {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.km-conv-context { display: flex; gap: 6px; align-items: center; }
.km-conv-tag {
  font-size: 10px;
  color: var(--kal-text-subtle);
  background: var(--kal-bg);
  padding: 2px 8px;
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}
.km-conv-expired {
  font-size: 10px;
  color: var(--kal-text-subtle);
  border: 1px solid var(--kal-border-strong);
  padding: 1px 6px;
  border-radius: var(--kal-radius-xs);
  letter-spacing: 1px;
}
.km-unread {
  position: absolute;
  top: 16px;
  right: 22px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--kal-primary-700);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: var(--kal-radius-full);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* ---------- Chat ---------- */
.km-chat { display: flex; flex-direction: column; }
.km-chat-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 28px;
  border-bottom: 1px solid var(--kal-divider);
}
.km-chat-name { font-weight: 600; font-size: var(--kal-text-md); color: var(--kal-text-strong); letter-spacing: 0.5px; }
.km-chat-context { font-size: 11px; color: var(--kal-text-subtle); margin-top: 2px; letter-spacing: 0.5px; }
.kh-icon-action {
  margin-left: auto;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--kal-border);
  background: transparent;
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-sm);
  cursor: pointer;
}
.kh-icon-action:hover { background: var(--kal-bg-subtle); color: var(--kal-text); }

.km-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-muted);
  padding: 10px 28px;
  font-size: var(--kal-text-sm);
  border-bottom: 1px solid var(--kal-divider);
  letter-spacing: 0.5px;
}

.km-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--kal-paper);
}
.km-system {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  align-self: stretch;
  justify-content: center;
}
.km-rule { width: 40px; height: 1px; background: var(--kal-divider); }
.km-msg {
  display: flex;
  gap: 10px;
  max-width: 70%;
}
.km-msg--them { align-self: flex-start; }
.km-msg--me { align-self: flex-end; flex-direction: row-reverse; }
.km-bubble-wrap { display: flex; flex-direction: column; gap: 4px; }
.km-msg--me .km-bubble-wrap { align-items: flex-end; }
.km-bubble {
  padding: 10px 16px;
  font-size: var(--kal-text-md);
  line-height: 1.7;
  border-radius: var(--kal-radius-md);
  word-break: break-word;
  letter-spacing: 0.3px;
}
.km-msg--them .km-bubble {
  background: #fff;
  color: var(--kal-text);
  border: 1px solid var(--kal-border);
  border-top-left-radius: 4px;
}
.km-msg--me .km-bubble {
  background: var(--kal-ink);
  color: #fff;
  border-top-right-radius: 4px;
}
.km-msg-time { font-size: 10px; color: var(--kal-text-subtle); letter-spacing: 0.5px; }

/* ---------- Input ---------- */
.km-input {
  border-top: 1px solid var(--kal-divider);
  padding: 14px 24px 18px;
  background: #fff;
}
.km-quicks { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.km-quick {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: var(--kal-bg-subtle);
  border: 1px solid transparent;
  border-radius: var(--kal-radius-full);
  font-size: 11px;
  color: var(--kal-text-muted);
  cursor: pointer;
  transition: all var(--kal-duration-2);
  letter-spacing: 0.5px;
}
.km-quick:hover {
  background: var(--kal-surface);
  border-color: var(--kal-border-strong);
  color: var(--kal-text);
}
.km-input-row { display: flex; gap: 12px; align-items: flex-end; }
.km-textarea {
  min-height: 60px;
  flex: 1;
  resize: none;
}
.km-send {
  padding: 0 20px;
  height: 60px;
  font-size: var(--kal-text-md);
  font-weight: 500;
  letter-spacing: 1px;
}

.km-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

@media (max-width: 768px) {
  .km { grid-template-columns: 1fr; height: calc(100vh - var(--kal-header-height) - 200px); }
  .km-list { display: none; }
  .km-h1 { font-size: 26px; }
}
</style>

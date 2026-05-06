<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const filter = reactive({ actor: '', action: '' })
const page = ref(1)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await adminApi.auditLogs({
      actor: filter.actor || undefined,
      action: filter.action || undefined,
      page: page.value, size: 30
    })
    list.value = r.items; total.value = r.total
  } catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)

async function search () { page.value = 1; await load() }
function fmt (t) { return t ? t.replace('T', ' ').slice(0, 19) : '' }
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / Audit</div>
        <h1 class="ad-page-title">审计日志</h1>
        <p class="ad-page-sub">所有管理员操作（登录、改状态、删除等）都会被记录在此。</p>
      </div>
    </header>

    <div class="ad-filter">
      <input class="kal-input" v-model="filter.actor" placeholder="按操作人 ID/名称" @keyup.enter="search" />
      <input class="kal-input" v-model="filter.action" placeholder="按动作类型（如 user_status）" @keyup.enter="search" />
      <button class="kal-btn kal-btn-secondary" @click="search"><Icon name="search" :size="14" /> 搜索</button>
    </div>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>时间</th><th>操作人</th><th>动作</th><th>对象</th><th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!list.length"><td colspan="5" class="ad-empty">暂无日志</td></tr>
          <tr v-for="a in list" :key="a.id">
            <td class="ad-mono">{{ fmt(a.createdAt) }}</td>
            <td>
              <div class="ad-name">{{ a.actorName || '—' }}</div>
              <div class="ad-sub">{{ a.actorId }}</div>
            </td>
            <td><span class="ad-tag">{{ a.action }}</span></td>
            <td>
              <div>{{ a.targetType || '—' }}</div>
              <div class="ad-sub">{{ a.targetId || '' }}</div>
            </td>
            <td class="ad-detail">{{ a.detail || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ad-pager">
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="ad-pager-info">第 {{ page }} 页 · 共 {{ total }} 条</span>
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page * 30 >= total" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>

<style scoped src="./admin-shared.css"></style>

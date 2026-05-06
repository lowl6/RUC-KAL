<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const filter = reactive({ keyword: '', competition: '', status: '' })
const page = ref(1)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await adminApi.listProjects({
      keyword: filter.keyword || undefined,
      competition: filter.competition || undefined,
      status: filter.status || undefined,
      page: page.value, size: 20
    })
    list.value = r.items; total.value = r.total
  } catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)

async function search () { page.value = 1; await load() }

async function setStatus (p, status) {
  const reason = status === 'hidden' || status === 'deleted'
    ? prompt(`将「${p.projectName}」状态置为 ${status}？请填写原因：`)
    : null
  if ((status === 'hidden' || status === 'deleted') && reason === null) return
  await adminApi.updateProjectStatus(p.projectId, status, reason)
  await load()
}

function statusLabel (s) {
  return ({ recruiting: '招募中', completed: '已完成', closed: '已关闭', hidden: '已隐藏', deleted: '已删除' })[s] || s
}
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / Projects</div>
        <h1 class="ad-page-title">项目卡治理</h1>
      </div>
    </header>

    <div class="ad-filter">
      <input class="kal-input" v-model="filter.keyword" placeholder="按标题/简介搜索" @keyup.enter="search" />
      <input class="kal-input" v-model="filter.competition" placeholder="比赛名（如：挑战杯）" @keyup.enter="search" />
      <select class="kal-input" v-model="filter.status" @change="search">
        <option value="">全部状态</option>
        <option value="recruiting">招募中</option>
        <option value="completed">已完成</option>
        <option value="closed">已关闭</option>
        <option value="hidden">已隐藏</option>
        <option value="deleted">已删除</option>
      </select>
      <button class="kal-btn kal-btn-secondary" @click="search"><Icon name="search" :size="14" /> 搜索</button>
    </div>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>项目</th><th>赛事</th><th>团队</th><th>关注</th><th>状态</th>
            <th style="width: 220px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!list.length"><td colspan="6" class="ad-empty">暂无项目</td></tr>
          <tr v-for="p in list" :key="p.projectId">
            <td>
              <div class="ad-name">{{ p.projectName }}</div>
              <div class="ad-sub">{{ p.oneLiner }}</div>
            </td>
            <td>
              <div>{{ p.competitionShort || '—' }}</div>
              <div class="ad-sub">{{ p.competitionTarget || '' }}</div>
            </td>
            <td>{{ p.currentMembers }}/{{ (p.currentMembers || 0) + (p.neededCount || 0) }}</td>
            <td>{{ p.viewCount }}<span class="ad-sub"> / 申请 {{ p.applyCount }}</span></td>
            <td><span class="ad-dot" :class="`is-${p.status}`"></span>{{ statusLabel(p.status) }}</td>
            <td class="ad-ops">
              <button v-if="p.status !== 'hidden'" class="ad-link-btn" @click="setStatus(p, 'hidden')">隐藏</button>
              <button v-else class="ad-link-btn" @click="setStatus(p, 'recruiting')">恢复</button>
              <button v-if="p.status !== 'deleted'" class="ad-link-btn ad-link-danger" @click="setStatus(p, 'deleted')">删除</button>
              <button v-else class="ad-link-btn" @click="setStatus(p, 'recruiting')">还原</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ad-pager">
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="ad-pager-info">第 {{ page }} 页 · 共 {{ total }} 条</span>
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page * 20 >= total" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>

<style scoped src="./admin-shared.css"></style>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { captchaApi } from '@/api/auth'
import Icon from '@/components/Icon.vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ id: '', code: '' }) },
  size: { type: String, default: 'md' }, // 'md' | 'sm'
})
const emit = defineEmits(['update:modelValue'])

const svg = ref('')
const id = ref('')
const code = ref(props.modelValue?.code || '')
const loading = ref(false)
const error = ref('')

watch(() => props.modelValue?.code, (v) => { code.value = v || '' })
watch(code, (v) => emit('update:modelValue', { id: id.value, code: v }))

async function refresh () {
  loading.value = true
  error.value = ''
  code.value = ''
  try {
    const r = await captchaApi.fetch()
    id.value = r.id
    svg.value = r.svg
    emit('update:modelValue', { id: id.value, code: '' })
  } catch (e) {
    error.value = e.message || '验证码加载失败'
  } finally {
    loading.value = false
  }
}

defineExpose({ refresh })
onMounted(refresh)
</script>

<template>
  <div class="kc">
    <input
      class="kal-input kc-input"
      :class="{ 'kc-input--sm': size === 'sm' }"
      v-model="code"
      :placeholder="size === 'sm' ? '4 位' : '请输入图中字符'"
      maxlength="6"
      autocomplete="off"
    />
    <button type="button" class="kc-canvas" :title="error || '点击换一张'" @click="refresh">
      <span v-if="loading" class="kc-loading">
        <Icon name="settings" :size="14" />
      </span>
      <span v-else-if="error" class="kc-error">
        <Icon name="alert" :size="14" />
      </span>
      <span v-else class="kc-svg" v-html="svg"></span>
    </button>
  </div>
</template>

<style scoped>
.kc {
  display: flex;
  align-items: stretch;
  gap: 10px;
}
.kc-input {
  flex: 1;
  letter-spacing: 4px;
  font-family: var(--kal-font-serif);
  text-align: left;
  text-transform: uppercase;
}
.kc-input--sm { letter-spacing: 2px; font-size: 14px; }

.kc-canvas {
  width: 132px;
  height: 44px;
  padding: 0;
  background: #fdf7f5;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  transition: border-color var(--kal-duration-2) var(--kal-ease-out);
}
.kc-canvas:hover { border-color: var(--kal-primary-600); }
.kc-svg, .kc-loading, .kc-error {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%; height: 100%;
}
.kc-svg :deep(svg) { width: 100%; height: 100%; display: block; }
.kc-loading {
  color: var(--kal-text-subtle);
  animation: kc-spin 1s linear infinite;
}
.kc-error { color: var(--kal-primary-700); }

@keyframes kc-spin { to { transform: rotate(360deg); } }
</style>

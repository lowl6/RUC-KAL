<script setup>
/**
 * 知行创坊 KAL · 统一图标组件
 *
 * 单一线条风格（1.5px stroke / round join / currentColor 继承）
 * 不依赖任何外部图标库，避免引入风格不一致的资源。
 *
 * 用法：<Icon name="search" :size="16" />
 */
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
  stroke: { type: [Number, String], default: 1.5 }
})

const sizePx = computed(() => `${props.size}px`)
</script>

<template>
  <svg
    class="kal-icon"
    :width="sizePx"
    :height="sizePx"
    viewBox="0 0 24 24"
    fill="none"
    :stroke-width="stroke"
    stroke="currentColor"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <!-- ============== Navigation ============== -->
    <g v-if="name === 'arrow-left'">
      <line x1="20" y1="12" x2="4" y2="12"/>
      <polyline points="10 18 4 12 10 6"/>
    </g>
    <g v-else-if="name === 'arrow-right'">
      <line x1="4" y1="12" x2="20" y2="12"/>
      <polyline points="14 6 20 12 14 18"/>
    </g>
    <g v-else-if="name === 'chevron-down'">
      <polyline points="6 9 12 15 18 9"/>
    </g>
    <g v-else-if="name === 'chevron-right'">
      <polyline points="9 6 15 12 9 18"/>
    </g>
    <g v-else-if="name === 'menu'">
      <line x1="4" y1="7" x2="20" y2="7"/>
      <line x1="4" y1="12" x2="20" y2="12"/>
      <line x1="4" y1="17" x2="14" y2="17"/>
    </g>
    <g v-else-if="name === 'close'">
      <line x1="6" y1="6" x2="18" y2="18"/>
      <line x1="18" y1="6" x2="6" y2="18"/>
    </g>

    <!-- ============== Action ============== -->
    <g v-else-if="name === 'plus'">
      <line x1="12" y1="5" x2="12" y2="19"/>
      <line x1="5" y1="12" x2="19" y2="12"/>
    </g>
    <g v-else-if="name === 'minus'">
      <line x1="5" y1="12" x2="19" y2="12"/>
    </g>
    <g v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="7"/>
      <line x1="20" y1="20" x2="16.5" y2="16.5"/>
    </g>
    <g v-else-if="name === 'edit'">
      <path d="M4 20h4l11-11-4-4L4 16v4z"/>
      <line x1="14" y1="6" x2="18" y2="10"/>
    </g>
    <g v-else-if="name === 'send'">
      <line x1="21" y1="3" x2="11" y2="13"/>
      <polygon points="21 3 14 21 11 13 3 10 21 3"/>
    </g>
    <g v-else-if="name === 'check'">
      <polyline points="5 12 10 17 19 7"/>
    </g>

    <!-- ============== Object ============== -->
    <g v-else-if="name === 'message'">
      <path d="M4 5h16a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H8l-4 4V6a1 1 0 0 1 1-1z"/>
    </g>
    <g v-else-if="name === 'mail'">
      <rect x="3" y="5" width="18" height="14" rx="1.5"/>
      <polyline points="3 7 12 13 21 7"/>
    </g>
    <g v-else-if="name === 'user'">
      <circle cx="12" cy="8" r="4"/>
      <path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"/>
    </g>
    <g v-else-if="name === 'users'">
      <circle cx="9" cy="8" r="3.5"/>
      <path d="M3 20c1-3 3.5-4.5 6-4.5s5 1.5 6 4.5"/>
      <path d="M16 4.5a3.5 3.5 0 0 1 0 7"/>
      <path d="M17 15.5c2 .5 3.5 2 4 4.5"/>
    </g>
    <g v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="9"/>
      <polyline points="12 7 12 12 15.5 13.5"/>
    </g>
    <g v-else-if="name === 'calendar'">
      <rect x="3.5" y="5" width="17" height="15" rx="1.5"/>
      <line x1="3.5" y1="10" x2="20.5" y2="10"/>
      <line x1="8" y1="3" x2="8" y2="7"/>
      <line x1="16" y1="3" x2="16" y2="7"/>
    </g>
    <g v-else-if="name === 'eye'">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>
      <circle cx="12" cy="12" r="2.5"/>
    </g>
    <g v-else-if="name === 'heart'">
      <path d="M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.5A4 4 0 0 1 19 10c0 5.5-7 10-7 10z"/>
    </g>
    <g v-else-if="name === 'star'">
      <polygon points="12 3 14.7 9 21 9.7 16.5 14 17.7 20.3 12 17.3 6.3 20.3 7.5 14 3 9.7 9.3 9"/>
    </g>
    <g v-else-if="name === 'sparkle'">
      <path d="M12 4l1.6 4.4L18 10l-4.4 1.6L12 16l-1.6-4.4L6 10l4.4-1.6z"/>
      <path d="M19 16l.7 1.8L21.5 18.5l-1.8.7L19 21l-.7-1.8L16.5 18.5l1.8-.7z" stroke-width="1.2"/>
    </g>
    <g v-else-if="name === 'trophy'">
      <path d="M8 4h8v3a4 4 0 1 1-8 0z"/>
      <path d="M8 7H5a1 1 0 0 0-1 1v1a3 3 0 0 0 3 3h1"/>
      <path d="M16 7h3a1 1 0 0 1 1 1v1a3 3 0 0 1-3 3h-1"/>
      <line x1="9" y1="13" x2="15" y2="13"/>
      <path d="M10 13l-1 4h6l-1-4"/>
      <line x1="7" y1="20" x2="17" y2="20"/>
    </g>
    <g v-else-if="name === 'compass'">
      <circle cx="12" cy="12" r="9"/>
      <polygon points="14.5 9.5 10 11 9.5 14.5 14 13"/>
    </g>
    <g v-else-if="name === 'flame'">
      <path d="M12 3c2 4 6 5 6 10a6 6 0 0 1-12 0c0-2 1-3 2-4 .5 2 2 2 2 0 0-3 1-5 2-6z"/>
    </g>
    <g v-else-if="name === 'pin'">
      <path d="M12 17v5"/>
      <path d="M9 11l-3 3 8 0 4-4-8-8-3 3 0 6"/>
    </g>
    <g v-else-if="name === 'tag'">
      <path d="M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9-9-9z"/>
      <circle cx="8" cy="8" r="1.5"/>
    </g>
    <g v-else-if="name === 'document'">
      <path d="M6 3h8l4 4v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/>
      <polyline points="14 3 14 8 19 8"/>
      <line x1="9" y1="13" x2="15" y2="13"/>
      <line x1="9" y1="17" x2="13" y2="17"/>
    </g>
    <g v-else-if="name === 'briefcase'">
      <rect x="3" y="7" width="18" height="13" rx="1.5"/>
      <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
      <line x1="3" y1="12" x2="21" y2="12"/>
    </g>
    <g v-else-if="name === 'target'">
      <circle cx="12" cy="12" r="9"/>
      <circle cx="12" cy="12" r="5"/>
      <circle cx="12" cy="12" r="1.5"/>
    </g>
    <g v-else-if="name === 'settings'">
      <circle cx="12" cy="12" r="3"/>
      <path d="M19.4 15a1.6 1.6 0 0 0 .3 1.7l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.7-.3 1.6 1.6 0 0 0-1 1.4V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.4 1.6 1.6 0 0 0-1.7.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.7 1.6 1.6 0 0 0-1.4-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.4-1 1.6 1.6 0 0 0-.3-1.7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.7.3H9a1.6 1.6 0 0 0 1-1.4V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.4 1.6 1.6 0 0 0 1.7-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.7V9a1.6 1.6 0 0 0 1.4 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.4 1z"/>
    </g>
    <g v-else-if="name === 'lock'">
      <rect x="5" y="11" width="14" height="10" rx="1.5"/>
      <path d="M8 11V7a4 4 0 1 1 8 0v4"/>
    </g>
    <g v-else-if="name === 'shield'">
      <path d="M12 3l8 3v6c0 4-3 8-8 9-5-1-8-5-8-9V6z"/>
    </g>
    <g v-else-if="name === 'info'">
      <circle cx="12" cy="12" r="9"/>
      <line x1="12" y1="11" x2="12" y2="16"/>
      <circle cx="12" cy="8" r=".6" fill="currentColor"/>
    </g>
    <g v-else-if="name === 'alert'">
      <path d="M12 3l10 18H2z"/>
      <line x1="12" y1="10" x2="12" y2="15"/>
      <circle cx="12" cy="18" r=".6" fill="currentColor"/>
    </g>
    <g v-else-if="name === 'logout'">
      <path d="M14 3h4a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1h-4"/>
      <polyline points="9 8 4 12 9 16"/>
      <line x1="4" y1="12" x2="14" y2="12"/>
    </g>
    <g v-else-if="name === 'megaphone'">
      <path d="M3 11v2a1 1 0 0 0 1 1h2l5 4V6L6 10H4a1 1 0 0 0-1 1z"/>
      <path d="M16 7c1 1.5 1.5 3 1.5 5s-.5 3.5-1.5 5"/>
      <path d="M19 4c2 2 3 5 3 8s-1 6-3 8"/>
    </g>
    <g v-else-if="name === 'link'">
      <path d="M10.5 13.5a4 4 0 0 0 5.6 0l3-3a4 4 0 1 0-5.6-5.6l-1.4 1.4"/>
      <path d="M13.5 10.5a4 4 0 0 0-5.6 0l-3 3a4 4 0 1 0 5.6 5.6l1.4-1.4"/>
    </g>
    <g v-else-if="name === 'refresh'">
      <polyline points="20 8 20 4 16 4"/>
      <path d="M20 4l-3.5 3.5a7 7 0 1 0 1.5 7"/>
    </g>

    <!-- ============== Domain (项目类型 / 角色 / 状态) ============== -->
    <g v-else-if="name === 'lightbulb'">
      <path d="M9 18h6"/>
      <path d="M10 21h4"/>
      <path d="M12 3a6 6 0 0 0-4 10.5c1 1 1.5 1.8 1.5 2.5h5c0-.7.5-1.5 1.5-2.5A6 6 0 0 0 12 3z"/>
    </g>
    <g v-else-if="name === 'hammer'">
      <path d="M14 4l6 6-3 3-6-6 3-3z"/>
      <path d="M11 7l-7 7v3l3 3 7-7"/>
    </g>
    <g v-else-if="name === 'rocket'">
      <path d="M5 19l4-2-2 4-2-2z"/>
      <path d="M14 6l4 4-7 7-4-4z"/>
      <path d="M14 6l3-3 4 4-3 3"/>
      <circle cx="15" cy="9" r="1.2"/>
    </g>

    <!-- Default: render question mark to make missing icon obvious -->
    <g v-else>
      <circle cx="12" cy="12" r="9"/>
      <path d="M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .8-1 1.5v.7"/>
      <circle cx="12" cy="17" r=".6" fill="currentColor"/>
    </g>
  </svg>
</template>

<style>
.kal-icon {
  flex-shrink: 0;
  vertical-align: middle;
  display: inline-block;
}
</style>

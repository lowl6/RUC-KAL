<script setup>
import { useRoute } from 'vue-router'
import { computed, watch, onBeforeUnmount } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const hideChrome = computed(() => route.meta?.layout === 'plain')

const auth = useAuthStore()
const user = useUserStore()

if (auth.isLoggedIn) user.startUnreadPolling()
watch(() => auth.isLoggedIn, (v) => {
  if (v) user.startUnreadPolling()
  else   user.stopUnreadPolling()
})
onBeforeUnmount(() => user.stopUnreadPolling())
</script>

<template>
  <div class="kal-app">
    <AppHeader v-if="!hideChrome" />
    <main class="kal-main" :class="{ 'kal-main--plain': hideChrome }">
      <RouterView v-slot="{ Component, route: r }">
        <Transition name="kal-page" mode="out-in">
          <component :is="Component" :key="r.fullPath" />
        </Transition>
      </RouterView>
    </main>
    <AppFooter v-if="!hideChrome" />
  </div>
</template>

<style>
.kal-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.kal-main {
  flex: 1;
  padding: 32px 0 64px;
}
.kal-main--plain {
  padding: 0;
  display: flex;
}
@media (max-width: 768px) {
  .kal-main { padding: 16px 0 32px; }
}
</style>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const navMenus = [
  { to: '/', label: '首页' },
  { to: '/competitions', label: '比赛中心' },
  { to: '/forum', label: '创坊论坛' },
  { to: '/me', label: '个人中心' }
]

const dropdownOpen = ref(false)
const userMenuOpen = ref(false)
const mobileOpen = ref(false)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function togglePublish() {
  dropdownOpen.value = !dropdownOpen.value
  userMenuOpen.value = false
}
function publishProject() { dropdownOpen.value = false; router.push('/projects/new') }
function publishProfile() { dropdownOpen.value = false; router.push('/personal-cards/edit') }
function publishPost()    { dropdownOpen.value = false; router.push('/forum') }

function toggleUserMenu() { userMenuOpen.value = !userMenuOpen.value; dropdownOpen.value = false }
function goLogout() { user.logout(); userMenuOpen.value = false; router.push('/login') }

function handleClickOutside(e) {
  if (!e.target.closest('.kh-publish') && !e.target.closest('.kh-user')) {
    dropdownOpen.value = false
    userMenuOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))

const totalUnread = computed(() => user.unreadMessages + user.unreadApplications + user.unreadReplies)
</script>

<template>
  <header class="kh">
    <div class="kal-container kh-inner">
      <!-- Logo -->
      <RouterLink to="/" class="kh-logo">
        <span class="kh-logo-mark">KAL</span>
        <span class="kh-logo-text">知行创坊</span>
        <span class="kh-logo-tag">KNOWACT&nbsp;LAB</span>
      </RouterLink>

      <!-- 桌面端导航 -->
      <nav class="kh-nav">
        <RouterLink
          v-for="m in navMenus"
          :key="m.to"
          :to="m.to"
          class="kh-nav-link"
          :class="{ 'kh-nav-link--active': isActive(m.to) }"
        >
          {{ m.label }}
        </RouterLink>
      </nav>

      <!-- 右侧操作区 -->
      <div class="kh-actions">
        <!-- 发布按钮 -->
        <div class="kh-publish">
          <button class="kal-btn kal-btn-sm kh-publish-btn" @click="togglePublish">
            <Icon name="plus" :size="14" :stroke="2" />
            <span>发布</span>
          </button>
          <Transition name="kh-pop">
            <div v-if="dropdownOpen" class="kh-dropdown">
              <button class="kh-dropdown-item" @click="publishProject">
                <span class="kh-dropdown-icon">
                  <Icon name="briefcase" :size="18" />
                </span>
                <span class="kh-dropdown-text">
                  <strong>发布项目卡</strong>
                  <small>组建队伍，写下招募意向</small>
                </span>
              </button>
              <button class="kh-dropdown-item" @click="publishProfile">
                <span class="kh-dropdown-icon">
                  <Icon name="target" :size="18" />
                </span>
                <span class="kh-dropdown-text">
                  <strong>发布个人卡</strong>
                  <small>陈列你的专长与节奏</small>
                </span>
              </button>
              <button class="kh-dropdown-item" @click="publishPost">
                <span class="kh-dropdown-icon">
                  <Icon name="message" :size="18" />
                </span>
                <span class="kh-dropdown-text">
                  <strong>论坛发帖</strong>
                  <small>分享见解，提出疑问</small>
                </span>
              </button>
            </div>
          </Transition>
        </div>

        <!-- 消息 -->
        <RouterLink to="/messages" class="kh-icon-btn" aria-label="私信">
          <Icon name="message" :size="18" />
          <span v-if="totalUnread" class="kh-badge">{{ totalUnread > 9 ? '9+' : totalUnread }}</span>
        </RouterLink>

        <!-- 用户头像 -->
        <div class="kh-user">
          <button class="kh-avatar-btn" @click="toggleUserMenu" aria-label="用户菜单">
            <span class="kal-avatar kal-avatar-sm">{{ user.initials }}</span>
          </button>
          <Transition name="kh-pop">
            <div v-if="userMenuOpen" class="kh-dropdown kh-dropdown--user">
              <div class="kh-user-info">
                <div class="kal-avatar">{{ user.initials }}</div>
                <div>
                  <div class="kh-user-name">{{ user.me?.display_name }}</div>
                  <div class="kh-user-dept">{{ user.me?.dept_name }} · {{ user.me?.grade }}</div>
                </div>
              </div>
              <div class="kh-divider"></div>
              <RouterLink to="/me" class="kh-dropdown-item kh-dropdown-item--simple" @click="userMenuOpen = false">
                <Icon name="user" :size="15" />
                <span>个人中心</span>
              </RouterLink>
              <RouterLink to="/personal-cards/edit" class="kh-dropdown-item kh-dropdown-item--simple" @click="userMenuOpen = false">
                <Icon name="target" :size="15" />
                <span>我的个人卡</span>
              </RouterLink>
              <div class="kh-divider"></div>
              <button class="kh-dropdown-item kh-dropdown-item--simple kh-logout" @click="goLogout">
                <Icon name="logout" :size="15" />
                <span>退出登录</span>
              </button>
            </div>
          </Transition>
        </div>

        <!-- 移动端菜单 -->
        <button class="kh-icon-btn kh-mobile-toggle" @click="mobileOpen = !mobileOpen" aria-label="菜单">
          <Icon name="menu" :size="20" />
        </button>
      </div>
    </div>

    <!-- 移动端导航抽屉 -->
    <Transition name="kh-mobile">
      <div v-if="mobileOpen" class="kh-mobile-menu">
        <RouterLink
          v-for="m in navMenus"
          :key="m.to"
          :to="m.to"
          class="kh-mobile-link"
          :class="{ 'kh-mobile-link--active': isActive(m.to) }"
          @click="mobileOpen = false"
        >
          {{ m.label }}
        </RouterLink>
      </div>
    </Transition>
  </header>
</template>

<style scoped>
.kh {
  position: sticky;
  top: 0;
  z-index: var(--kal-z-header);
  background: rgba(246, 243, 238, 0.78);
  backdrop-filter: saturate(180%) blur(18px);
  -webkit-backdrop-filter: saturate(180%) blur(18px);
  border-bottom: 1px solid var(--kal-border);
}
.kh-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--kal-header-height);
  gap: 24px;
}

/* Logo */
.kh-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  user-select: none;
}
.kh-logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--ruc-red);
  color: #fff;
  border-radius: var(--kal-radius-sm);
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 1.5px;
}
.kh-logo-text {
  font-family: var(--kal-font-serif);
  font-weight: var(--kal-fw-semibold);
  font-size: 18px;
  color: var(--kal-text-strong);
  letter-spacing: 4px;
}
.kh-logo-tag {
  font-size: 10px;
  color: var(--kal-text-subtle);
  letter-spacing: 3px;
  margin-left: 2px;
  padding-left: 12px;
  border-left: 1px solid var(--kal-border-strong);
  font-weight: 500;
}

/* Nav */
.kh-nav {
  display: flex;
  gap: 2px;
  flex: 1;
  margin-left: 40px;
}
.kh-nav-link {
  position: relative;
  padding: 8px 16px;
  font-size: var(--kal-text-md);
  font-weight: var(--kal-fw-medium);
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-sm);
  transition: color var(--kal-duration-2) var(--kal-ease-out);
}
.kh-nav-link:hover { color: var(--kal-text-strong); }
.kh-nav-link::after {
  content: '';
  position: absolute;
  left: 16px; right: 16px;
  bottom: 4px;
  height: 1px;
  background: currentColor;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--kal-duration-3) var(--kal-ease-out);
}
.kh-nav-link--active {
  color: var(--kal-text-strong);
  font-weight: var(--kal-fw-semibold);
}
.kh-nav-link--active::after {
  transform: scaleX(1);
  background: var(--kal-primary-600);
  height: 1.5px;
}

/* Actions */
.kh-actions { display: flex; align-items: center; gap: 8px; }
.kh-icon-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-sm);
  transition: all var(--kal-duration-2) var(--kal-ease-out);
}
.kh-icon-btn:hover { background: var(--kal-bg-subtle); color: var(--kal-text-strong); }
.kh-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--kal-primary-700);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: var(--kal-radius-full);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--kal-bg);
  letter-spacing: 0;
}

/* Publish */
.kh-publish { position: relative; }
.kh-publish-btn {
  background: var(--ruc-red);
  border-color: var(--ruc-red);
  font-weight: var(--kal-fw-medium);
  letter-spacing: 1px;
  padding-left: 14px;
  padding-right: 16px;
}
.kh-publish-btn:hover { background: var(--kal-ink-soft); border-color: var(--kal-ink-soft); }

/* Dropdown */
.kh-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 260px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  box-shadow: var(--kal-shadow-lg);
  padding: 6px;
  z-index: var(--kal-z-dropdown);
}
.kh-dropdown--user { min-width: 240px; }
.kh-dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-radius: var(--kal-radius-sm);
  text-align: left;
  cursor: pointer;
  transition: background var(--kal-duration-1);
  color: var(--kal-text);
}
.kh-dropdown-item:hover { background: var(--kal-bg-subtle); }
.kh-dropdown-text strong { display: block; font-size: var(--kal-text-md); font-weight: 600; margin-bottom: 2px; color: var(--kal-text-strong); }
.kh-dropdown-text small { display: block; font-size: var(--kal-text-xs); color: var(--kal-text-subtle); }
.kh-dropdown-item--simple { padding: 9px 12px; font-size: var(--kal-text-md); }
.kh-dropdown-item--simple .kal-icon { color: var(--kal-text-subtle); }
.kh-dropdown-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--kal-radius-sm);
  background: var(--kal-bg-subtle);
  color: var(--kal-primary-700);
  flex-shrink: 0;
}
.kh-divider { height: 1px; background: var(--kal-divider); margin: 6px 4px; }

.kh-user-info {
  display: flex;
  gap: 12px;
  padding: 10px 12px 14px;
}
.kh-user-name { font-weight: 600; color: var(--kal-text-strong); font-size: var(--kal-text-md); letter-spacing: 0.5px; }
.kh-user-dept { color: var(--kal-text-subtle); font-size: var(--kal-text-xs); margin-top: 2px; }
.kh-logout { color: var(--kal-primary-700); }

.kh-avatar-btn { background: transparent; border: none; padding: 0; }

/* Pop animation */
.kh-pop-enter-active, .kh-pop-leave-active {
  transition: all 180ms var(--kal-ease-out);
  transform-origin: top right;
}
.kh-pop-enter-from { opacity: 0; transform: scale(0.96) translateY(-4px); }
.kh-pop-leave-to   { opacity: 0; transform: scale(0.97); }

/* 移动端 */
.kh-mobile-toggle { display: none; }
.kh-mobile-menu {
  display: none;
  flex-direction: column;
  padding: 12px 16px 16px;
  gap: 4px;
  border-top: 1px solid var(--kal-border);
  background: var(--kal-surface);
}
.kh-mobile-link {
  padding: 12px 16px;
  border-radius: var(--kal-radius-sm);
  font-weight: 500;
  color: var(--kal-text);
}
.kh-mobile-link--active { background: var(--kal-bg-subtle); color: var(--kal-primary-700); }
.kh-mobile-enter-active, .kh-mobile-leave-active { transition: all 220ms ease; overflow: hidden; }
.kh-mobile-enter-from, .kh-mobile-leave-to { max-height: 0; opacity: 0; }
.kh-mobile-enter-to, .kh-mobile-leave-from { max-height: 320px; opacity: 1; }

@media (max-width: 1024px) {
  .kh-logo-tag { display: none; }
  .kh-nav { margin-left: 24px; }
}
@media (max-width: 768px) {
  .kh-nav, .kh-publish-btn span { display: none; }
  .kh-mobile-toggle { display: inline-flex; }
  .kh-mobile-menu { display: flex; }
  .kh-logo-text { font-size: 16px; }
  .kh-publish-btn { padding: 6px 10px; }
}
</style>

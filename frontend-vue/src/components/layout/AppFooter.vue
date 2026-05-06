<script setup>
import { RouterLink } from 'vue-router'
const year = new Date().getFullYear()
</script>

<template>
  <footer class="kf">
    <div class="kal-container kf-inner">
      <div class="kf-brand">
        <span class="kf-mark">KAL</span>
        <div>
          <div class="kf-title">知行创坊 · KNOWACT&nbsp;LAB</div>
          <div class="kf-sub">中国人民大学三创合作之所</div>
        </div>
      </div>
      <nav class="kf-links">
        <RouterLink to="/about"   class="kf-link">关于</RouterLink>
        <RouterLink to="/help"    class="kf-link">使用指南</RouterLink>
        <RouterLink to="/privacy" class="kf-link">隐私协议</RouterLink>
        <RouterLink to="/contact" class="kf-link">联系我们</RouterLink>
      </nav>
      <div class="kf-copy">© {{ year }} &nbsp;Renmin University of China</div>
    </div>
  </footer>
</template>

<style scoped>
.kf {
  margin-top: auto;
  background: var(--kal-surface);
  border-top: 1px solid var(--kal-border);
  padding: 36px 0 32px;
}
.kf-inner {
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
  text-align: center;
}
.kf-brand { display: flex; align-items: center; gap: 14px; }
.kf-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--ruc-red);
  color: #fff;
  border-radius: var(--kal-radius-sm);
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 1.5px;
}
.kf-title {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-base);
  color: var(--kal-text-strong);
  letter-spacing: 2px;
  text-align: left;
}
.kf-sub { font-size: var(--kal-text-xs); color: var(--kal-text-subtle); text-align: left; letter-spacing: 1.5px; margin-top: 2px; }
.kf-links { display: flex; flex-wrap: wrap; gap: 32px; }
.kf-link { font-size: var(--kal-text-sm); color: var(--kal-text-muted); transition: color 180ms; letter-spacing: 0.5px; }
.kf-link:hover { color: var(--kal-primary-700); }
.kf-copy { font-size: var(--kal-text-xs); color: var(--kal-text-subtle); letter-spacing: 1.5px; }

@media (min-width: 768px) {
  .kf-inner { flex-direction: row; justify-content: space-between; text-align: left; }
}
</style>

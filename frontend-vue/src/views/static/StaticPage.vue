<script setup>
import Icon from '@/components/Icon.vue'

defineProps({
  eyebrow: { type: String, default: '知行创坊' },
  title:   { type: String, required: true },
  lead:    { type: String, default: '' },
  updated: { type: String, default: '' }
})
</script>

<template>
  <div class="kal-container sp-wrap">
    <header class="sp-hero">
      <div class="sp-eyebrow">
        <span class="sp-bar"></span>
        <span>{{ eyebrow }}</span>
      </div>
      <h1 class="sp-title">{{ title }}</h1>
      <p v-if="lead" class="sp-lead">{{ lead }}</p>
      <div v-if="updated" class="sp-updated">
        <Icon name="settings" :size="12" />
        <span>{{ updated }}</span>
      </div>
    </header>

    <section class="sp-body">
      <slot />
    </section>
  </div>
</template>

<style scoped>
.sp-wrap { padding: 56px 0 80px; }

.sp-hero {
  position: relative;
  padding: 32px 36px 36px;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-xl);
  background:
    linear-gradient(180deg, var(--kal-primary-50) 0%, transparent 78%),
    var(--kal-surface);
  overflow: hidden;
}
.sp-hero::before {
  content: '';
  position: absolute;
  top: 0; left: 0; bottom: 0;
  width: 4px;
  background: var(--ruc-red);
}

.sp-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ruc-red);
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xs);
  letter-spacing: 4px;
  text-transform: uppercase;
}
.sp-bar {
  display: inline-block;
  width: 24px;
  height: 1px;
  background: var(--ruc-red);
}

.sp-title {
  margin: 14px 0 0;
  font-family: var(--kal-font-serif);
  font-weight: 700;
  font-size: var(--kal-text-5xl);
  letter-spacing: 4px;
  color: var(--kal-text-strong);
}

.sp-lead {
  margin: 16px 0 0;
  max-width: 64ch;
  font-size: var(--kal-text-lg);
  color: var(--kal-text-muted);
  line-height: 1.85;
  letter-spacing: 0.4px;
}

.sp-updated {
  margin-top: 20px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xs);
  letter-spacing: 1.5px;
  color: var(--kal-text-subtle);
}

.sp-body {
  margin-top: 36px;
  display: grid;
  gap: 24px;
}

:slotted(section.sp-card) {
  padding: 28px 32px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
}

:slotted(section.sp-card > h2) {
  margin: 0 0 16px;
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-2xl);
  letter-spacing: 2px;
  color: var(--kal-text-strong);
  display: flex;
  align-items: center;
  gap: 12px;
}
:slotted(section.sp-card > h2)::before {
  content: '';
  display: inline-block;
  width: 14px;
  height: 1px;
  background: var(--ruc-red);
}

:slotted(section.sp-card > h3) {
  margin: 22px 0 10px;
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-lg);
  letter-spacing: 1.5px;
  color: var(--kal-text-strong);
}

:slotted(section.sp-card p),
:slotted(section.sp-card li) {
  font-size: var(--kal-text-md);
  line-height: 1.95;
  color: var(--kal-text);
  letter-spacing: 0.3px;
}

:slotted(section.sp-card ul),
:slotted(section.sp-card ol) {
  padding-left: 22px;
  margin: 8px 0;
}

:slotted(section.sp-card ul li),
:slotted(section.sp-card ol li) {
  margin-bottom: 6px;
}

:slotted(section.sp-card a) {
  color: var(--ruc-red);
  border-bottom: 1px dashed var(--kal-primary-200);
  transition: color 180ms;
}
:slotted(section.sp-card a:hover) {
  color: var(--ruc-red-dark);
  border-bottom-color: var(--ruc-red);
}

:slotted(.sp-grid) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 12px;
}

:slotted(.sp-step) {
  padding: 18px;
  background: var(--kal-surface-2);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
}
:slotted(.sp-step .sp-step-no) {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xs);
  letter-spacing: 3px;
  color: var(--ruc-red);
}
:slotted(.sp-step .sp-step-title) {
  margin-top: 6px;
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-base);
  color: var(--kal-text-strong);
  letter-spacing: 1px;
}
:slotted(.sp-step .sp-step-desc) {
  margin-top: 8px;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  line-height: 1.85;
}

:slotted(.sp-pair) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  background: var(--kal-surface-2);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
}
:slotted(.sp-pair .sp-pair-key) {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xs);
  letter-spacing: 3px;
  color: var(--kal-text-subtle);
  text-transform: uppercase;
}
:slotted(.sp-pair .sp-pair-val) {
  font-size: var(--kal-text-md);
  color: var(--kal-text-strong);
  letter-spacing: 0.3px;
}

@media (max-width: 768px) {
  .sp-wrap { padding: 32px 0 56px; }
  .sp-hero { padding: 24px 22px 28px; }
  .sp-title { font-size: var(--kal-text-3xl); letter-spacing: 2px; }
  :slotted(section.sp-card) { padding: 22px 20px; }
}
</style>

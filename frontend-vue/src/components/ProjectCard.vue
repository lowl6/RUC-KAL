<script setup>
import Icon from '@/components/Icon.vue'

defineProps({
  project: { type: Object, required: true }
})
defineEmits(['contact'])
</script>

<template>
  <article class="kal-card kal-card-hoverable pc">
    <header class="pc-meta-top">
      <span class="pc-comp">{{ project.competition_short }}</span>
      <span class="pc-deadline" :class="{ 'pc-deadline--urgent': project.badge === 'urgent' }">
        <Icon name="clock" :size="11" />
        <span>{{ project.days_left }}天后截止</span>
      </span>
    </header>

    <h3 class="pc-title">{{ project.project_name }}</h3>
    <p class="pc-subtitle">{{ project.one_liner }}</p>

    <div class="pc-meta">
      <span class="pc-meta-item">
        <Icon name="users" :size="13" />
        <span>已有 {{ project.current_members }} · 招募 {{ project.needed_count }}</span>
      </span>
      <span class="pc-meta-item">
        <Icon name="clock" :size="13" />
        <span>每周 {{ project.weekly_hours }} 小时</span>
      </span>
    </div>

    <div class="pc-tags">
      <span
        v-for="tag in project.tags.slice(0, 4)"
        :key="tag"
        class="pc-tag"
      >{{ tag }}</span>
    </div>

    <footer class="pc-footer">
      <div class="pc-author">
        <span class="kal-avatar kal-avatar-sm">{{ project.creator.initial }}</span>
        <div class="pc-author-text">
          <div class="pc-author-name">{{ project.creator.name }}</div>
          <div class="pc-author-dept">{{ project.creator.dept }} · {{ project.creator.grade }}</div>
        </div>
      </div>
      <button class="kal-btn kal-btn-sm kal-btn-secondary pc-btn" @click.stop="$emit('contact', project)">
        <Icon name="message" :size="13" />
        <span>私信</span>
      </button>
    </footer>
  </article>
</template>

<style scoped>
.pc {
  padding: 24px 24px 20px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}
.pc-meta-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 11px;
  letter-spacing: 1.5px;
}
.pc-comp {
  color: var(--kal-text-strong);
  font-weight: 600;
  text-transform: uppercase;
}
.pc-deadline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--kal-text-muted);
  letter-spacing: 0.5px;
  font-size: var(--kal-text-xs);
}
.pc-deadline--urgent { color: var(--kal-primary-700); font-weight: 500; }

.pc-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--kal-text-strong);
  line-height: 1.4;
  margin-bottom: 10px;
}
.pc-subtitle {
  font-size: var(--kal-text-md);
  color: var(--kal-text-muted);
  line-height: 1.7;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pc-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  margin-bottom: 14px;
}
.pc-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
}

.pc-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 18px; }
.pc-tag {
  display: inline-flex;
  padding: 2px 10px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-muted);
  font-size: 11px;
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}
.pc-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--kal-divider);
  margin-top: auto;
  gap: 12px;
}
.pc-author { display: flex; align-items: center; gap: 10px; min-width: 0; }
.pc-author-name {
  font-weight: 500;
  font-size: var(--kal-text-sm);
  color: var(--kal-text);
  letter-spacing: 0.5px;
}
.pc-author-dept {
  font-size: 11px;
  color: var(--kal-text-subtle);
  margin-top: 2px;
  letter-spacing: 0.5px;
}
.pc-btn { padding: 6px 14px; }
</style>

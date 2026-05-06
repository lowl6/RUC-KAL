<script setup>
import Icon from '@/components/Icon.vue'

defineProps({
  card: { type: Object, required: true }
})
defineEmits(['invite'])
</script>

<template>
  <article class="kal-card kal-card-hoverable pcard">
    <header class="pcard-header">
      <span class="kal-avatar kal-avatar-lg">{{ card.initial }}</span>
      <div class="pcard-info">
        <div class="pcard-name">{{ card.display_name }}</div>
        <div class="pcard-meta">
          <span>{{ card.dept_name }}</span>
          <span class="pcard-dot"></span>
          <span>{{ card.grade }}</span>
        </div>
        <div class="pcard-role">{{ card.target_role }}</div>
      </div>
      <span v-if="card.is_new" class="pcard-new">NEW</span>
    </header>

    <hr class="kal-rule pcard-rule" />

    <p class="pcard-intro">{{ card.self_intro }}</p>

    <div class="pcard-skills">
      <span v-for="skill in card.skills" :key="skill" class="pcard-skill">{{ skill }}</span>
    </div>

    <div class="pcard-stats">
      <span class="pcard-stat">
        <Icon name="clock" :size="13" />
        <span>每周 {{ card.weekly_hours }} 小时</span>
      </span>
      <span v-if="card.vacation_available" class="pcard-stat">
        <Icon name="calendar" :size="13" />
        <span>寒暑假可投入</span>
      </span>
    </div>

    <footer class="pcard-footer">
      <div class="pcard-interest">
        <Icon name="trophy" :size="13" />
        <span>{{ card.interested_competitions.join(' · ') }}</span>
      </div>
      <button class="kal-btn kal-btn-sm pcard-btn" @click.stop="$emit('invite', card)">
        邀请加入
      </button>
    </footer>
  </article>
</template>

<style scoped>
.pcard {
  padding: 24px;
  display: flex;
  flex-direction: column;
}
.pcard-header { display: flex; gap: 14px; align-items: flex-start; }
.pcard-info { flex: 1; min-width: 0; }
.pcard-name {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1.5px;
  margin-bottom: 4px;
}
.pcard-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 6px;
}
.pcard-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }
.pcard-role {
  display: inline-flex;
  font-size: var(--kal-text-sm);
  color: var(--kal-primary-700);
  font-weight: 500;
  letter-spacing: 0.5px;
}
.pcard-new {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  background: var(--kal-ink);
  color: #fff;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 1.5px;
  border-radius: var(--kal-radius-xs);
}

.pcard-rule { margin: 18px 0; }

.pcard-intro {
  font-size: var(--kal-text-md);
  color: var(--kal-text-muted);
  line-height: 1.75;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pcard-skills { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
.pcard-skill {
  display: inline-flex;
  padding: 3px 10px;
  font-size: 11px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}

.pcard-stats {
  display: flex;
  gap: 18px;
  margin-bottom: 16px;
  font-size: var(--kal-text-sm);
}
.pcard-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--kal-text-muted);
}

.pcard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--kal-divider);
  gap: 12px;
  margin-top: auto;
}
.pcard-interest {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  min-width: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pcard-btn { padding: 6px 14px; letter-spacing: 1px; }
</style>

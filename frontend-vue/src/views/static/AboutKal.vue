<script setup>
import StaticPage from './StaticPage.vue'
</script>

<template>
  <StaticPage
    eyebrow="ABOUT · 关于"
    title="知行创坊"
    lead="知行创坊（KNOWACT Lab，简称 KAL）是面向中国人民大学师生的轻量级三创合作平台。我们尝试在「学术训练」与「真实项目」之间搭一条短桥：把想法写成卡片、找到合适的人、按比赛节奏跑完一个完整周期。"
    updated="最近更新：2026 年 5 月"
  >
    <section class="sp-card">
      <h2>我们在做什么</h2>
      <p>
        知行创坊把校园里分散的「项目卡 / 个人卡 / 比赛信息 / 论坛讨论」收拢到一处。
        你可以把自己的项目挂出来招队友，也可以把自己介绍清楚等别人来邀；
        看见合适的人，先在站内私信，确认彼此目标契合，再决定是否互换微信、组队、报赛。
      </p>
      <p>
        我们不是 LinkedIn，也不是简历库。我们更看重「同步在做的事」，而不是「以前做过的事」。
      </p>
    </section>

    <section class="sp-card">
      <h2>核心场景</h2>
      <div class="sp-grid">
        <div class="sp-step">
          <div class="sp-step-no">SCENE&nbsp;01</div>
          <div class="sp-step-title">发布项目卡</div>
          <div class="sp-step-desc">把你的项目方向、目标比赛、缺位与所需技能写清楚，让感兴趣的同学直接看见。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">SCENE&nbsp;02</div>
          <div class="sp-step-title">维护个人卡</div>
          <div class="sp-step-desc">写下你想承担的角色、可投入的小时数、想参与的比赛——而不是堆砌过去的奖项。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">SCENE&nbsp;03</div>
          <div class="sp-step-title">站内私信</div>
          <div class="sp-step-desc">先在站内沟通：聊得来再换微信。系统会按比赛节奏给你保留一段对话窗口。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">SCENE&nbsp;04</div>
          <div class="sp-step-title">赛事 / 资讯</div>
          <div class="sp-step-desc">主流赛事按报名截止时间排序，状态由系统按日期自动维护：进行中 / 即将截止 / 已结束。</div>
        </div>
      </div>
    </section>

    <section class="sp-card">
      <h2>谁在维护这个平台</h2>
      <p>
        知行创坊由中国人民大学师生组成的志愿团队搭建与维护，得到学校就业创业相关部门在赛事信息收录方面的支持。
        平台所有功能均面向校内学习与组队场景。
      </p>
      <p>
        常见问题、规则、申诉路径，请见
        <RouterLink to="/help">使用指南</RouterLink>；
        信息处理范围请见
        <RouterLink to="/privacy">隐私协议</RouterLink>；
        其它合作或反馈请见
        <RouterLink to="/contact">联系我们</RouterLink>。
      </p>
    </section>
  </StaticPage>
</template>

<script>
import { RouterLink } from 'vue-router'
export default { components: { RouterLink } }
</script>

<script setup>
import StaticPage from './StaticPage.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <StaticPage
    eyebrow="GUIDE · 使用指南"
    title="如何用好知行创坊"
    lead="把「找队友 / 找项目 / 跟比赛」三件事拆成最少的步骤。如果你只准备读其中一段，就读「最常见的 4 个动作」。"
    updated="最近更新：2026 年 5 月"
  >
    <section class="sp-card">
      <h2>注册与登录</h2>
      <h3>校内同学</h3>
      <p>使用 <code>@ruc.edu.cn</code> 校园邮箱注册。注册流程包括：填写姓名 / 学院 / 学号 → 输入邮箱 → 点击「发送验证码」→ 在邮件中复制 6 位验证码回填 → 通过图形验证码后提交。</p>
      <p>密码长度需 ≥ 8 位，建议同时包含字母与数字。注册成功后会直接进入首页。</p>
      <h3>没有校内邮箱的合作者</h3>
      <p>校外合作者的账号由管理员代开。请通过 <RouterLink to="/contact">联系我们</RouterLink> 提交需求；管理员开通后，你会收到带有「初始登录密码」的邮件，首次登录后请立即在「我的中心 → 修改密码」中改成自己的密码。</p>
    </section>

    <section class="sp-card">
      <h2>最常见的 4 个动作</h2>
      <div class="sp-grid">
        <div class="sp-step">
          <div class="sp-step-no">STEP&nbsp;01</div>
          <div class="sp-step-title">发一张项目卡</div>
          <div class="sp-step-desc">在导航栏点「发布项目卡」。建议把：目标比赛 / 缺位角色 / 每周可投入小时数 / 截止时间，写得越具体越能被精确匹配。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">STEP&nbsp;02</div>
          <div class="sp-step-title">写好个人卡</div>
          <div class="sp-step-desc">在「我的中心 → 我的个人卡」编辑。包含：想承担的角色、技能标签、感兴趣的比赛、是否假期可投入。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">STEP&nbsp;03</div>
          <div class="sp-step-title">私信沟通</div>
          <div class="sp-step-desc">看见合适的人 / 项目，点「私信」开聊。可以在「我的中心 → 通知设置」里打开「新私信邮件提醒」。</div>
        </div>
        <div class="sp-step">
          <div class="sp-step-no">STEP&nbsp;04</div>
          <div class="sp-step-title">关注比赛节奏</div>
          <div class="sp-step-desc">「比赛中心」按报名截止时间排序。状态由系统按日期自动维护：upcoming / active / urgent / ended。</div>
        </div>
      </div>
    </section>

    <section class="sp-card">
      <h2>常见问题</h2>
      <h3>1. 收不到注册邮件验证码？</h3>
      <ul>
        <li>检查垃圾邮件 / 广告邮件文件夹（Outlook、QQ 邮箱常见）。</li>
        <li>验证码 60 秒内只能发送一次，请等待倒计时结束再重试。</li>
        <li>如多次未收到，请在 <RouterLink to="/contact">联系我们</RouterLink> 中告知你尝试注册的邮箱。</li>
      </ul>
      <h3>2. 项目里的「比赛状态」是怎么算的？</h3>
      <ul>
        <li>管理员只需录入「报名开始日期」「报名截止日期」。</li>
        <li>系统每天 0 点 5 分自动刷新，并在管理员保存赛事时立即派生：现在 &lt; 开始 → upcoming；进行中且距截止 ≤ 7 天 → urgent；已过截止 → ended。</li>
        <li>如果你看到状态明显不对，多半是日期填错，请联系赛事发布的管理员。</li>
      </ul>
      <h3>3. 私信能保存多久？</h3>
      <p>私信会一直保留，但「未读小红点」与「邮件提醒」是基于会话最近一次「读到这里」的时间计算的。打开会话即视为已读。</p>
      <h3>4. 登录提示「图形验证码错误」？</h3>
      <p>每张图形验证码 5 分钟有效；若多次刷新仍失败，可点击图形上的刷新区域重新生成。</p>
      <h3>5. 想下线一个项目卡 / 个人卡？</h3>
      <p>在「我的中心」对应卡片上选择「下线」。下线后他人无法搜索到，但站内已有的私信不受影响。</p>
    </section>

    <section class="sp-card">
      <h2>社区行为准则</h2>
      <ul>
        <li>真实表达：不要伪造身份或所在院系；个人卡里的可投入时间请贴近实际。</li>
        <li>尊重他人：禁止在私信、论坛中发布人身攻击、骚扰、广告或与三创无关的拉群信息。</li>
        <li>保护隐私：不要在公开页面（项目卡 / 论坛）写他人手机号、住址等敏感信息。</li>
        <li>遵守学校 / 比赛规则：参赛信息、奖项以官方公告为准，本平台仅作汇总。</li>
      </ul>
      <p>违反以上原则的内容，管理员可下线、对账号做封禁处理；申诉路径请见 <RouterLink to="/contact">联系我们</RouterLink>。</p>
    </section>
  </StaticPage>
</template>

<style scoped>
:deep(code) {
  padding: 1px 6px;
  background: var(--kal-primary-50);
  border-radius: 4px;
  color: var(--ruc-red-dark);
  font-family: var(--kal-font-mono);
  font-size: 0.92em;
}
</style>

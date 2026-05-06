<script setup>
import StaticPage from './StaticPage.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <StaticPage
    eyebrow="PRIVACY · 隐私协议"
    title="知行创坊隐私协议"
    lead="本协议描述知行创坊（KAL）在为校内师生提供三创合作服务时，处理你个人信息的方式。我们遵循「最少必要」原则：只收为达成功能所必需的信息，不向第三方出售你的数据。"
    updated="生效日期：2026 年 5 月 1 日"
  >
    <section class="sp-card">
      <h2>一、我们处理哪些信息</h2>
      <h3>1. 注册与认证</h3>
      <ul>
        <li>校园邮箱（<code>@ruc.edu.cn</code>）或由管理员代开账号时绑定的合作邮箱。</li>
        <li>姓名 / 学院 / 学号（用于真实身份与同侪定位）。</li>
        <li>密码（仅以 BCrypt 单向哈希存储，平台与管理员均无法获取明文）。</li>
      </ul>
      <h3>2. 资料发布</h3>
      <ul>
        <li>项目卡、个人卡中由你主动填写的内容。</li>
        <li>论坛帖子、回复、点赞记录。</li>
      </ul>
      <h3>3. 互动行为</h3>
      <ul>
        <li>私信内容、收发时间、会话「读到这里」的时间戳（用于未读提醒）。</li>
        <li>访问、点击、报名按钮等聚合统计；不与个人身份合并对外披露。</li>
      </ul>
      <h3>4. 通知</h3>
      <ul>
        <li>当你在「我的中心 → 通知设置」中主动开启邮件提醒时，平台会向注册邮箱发送私信摘要邮件。</li>
        <li>关闭后立即停止；历史邮件不会被追回。</li>
      </ul>
    </section>

    <section class="sp-card">
      <h2>二、我们不做什么</h2>
      <ul>
        <li>不向广告主、第三方公司出售或交换你的个人信息。</li>
        <li>不在公开页面展示你的邮箱、手机号、登录 IP 等敏感字段。</li>
        <li>不读取你与外部同学交换的微信对话；私信内容仅在本站内部存储。</li>
        <li>不把你的项目卡内容用于训练任何商业 AI 模型。</li>
      </ul>
    </section>

    <section class="sp-card">
      <h2>三、信息存储与安全</h2>
      <ul>
        <li>密码：BCrypt 哈希，登录时验证哈希一致性。</li>
        <li>会话：基于 JWT 颁发的访问令牌；过期或登出后立即失效。</li>
        <li>验证码：图形验证码与邮箱验证码在内存中临时缓存，超过 TTL 自动作废。</li>
        <li>数据库：默认部署在校内服务器；运维与备份由学校信息技术中心审查。</li>
      </ul>
    </section>

    <section class="sp-card">
      <h2>四、你的权利</h2>
      <ul>
        <li><strong>查看 / 修改</strong>：在「我的中心」可随时查看与修改个人信息、项目卡、个人卡。</li>
        <li><strong>下线 / 删除</strong>：项目卡、个人卡可下线；如希望永久注销账号，请通过 <RouterLink to="/contact">联系我们</RouterLink> 提交申请，3 个工作日内处理。</li>
        <li><strong>知情</strong>：当你的内容被管理员下线、账号被封禁时，你会收到站内通知与原因说明。</li>
        <li><strong>申诉</strong>：对管理员的处置有异议，可在 <RouterLink to="/contact">联系我们</RouterLink> 中提交申诉。</li>
      </ul>
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

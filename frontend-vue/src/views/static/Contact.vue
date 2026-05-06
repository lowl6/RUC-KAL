<script setup>
import { ref } from 'vue'
import StaticPage from './StaticPage.vue'
import Icon from '@/components/Icon.vue'

const copied = ref('')
async function copy (key, text) {
  try {
    await navigator.clipboard.writeText(text)
    copied.value = key
    setTimeout(() => (copied.value = ''), 1500)
  } catch (e) {
    /* ignore */
  }
}
</script>

<template>
  <StaticPage
    eyebrow="CONTACT · 联系我们"
    title="把问题、建议、合作意向告诉我们"
    lead="知行创坊由学生志愿团队运营，回复速度通常在 1–3 个工作日。请尽量在邮件中说明你的身份（学院 + 年级）与诉求类型，便于我们准确分流。"
    updated="最近更新：2026 年 5 月"
  >
    <section class="sp-card">
      <h2>官方联系方式</h2>
      <div class="sp-grid">
        <div class="sp-pair">
          <span class="sp-pair-key">官方邮箱</span>
          <span class="sp-pair-val">
            <a href="mailto:RUC_KAL@163.com">RUC_KAL@163.com</a>
            <button class="ct-copy" @click="copy('mail', 'RUC_KAL@163.com')">
              <Icon name="message" :size="12" />
              <span>{{ copied === 'mail' ? '已复制' : '复制' }}</span>
            </button>
          </span>
        </div>
        <div class="sp-pair">
          <span class="sp-pair-key">校内地址</span>
          <span class="sp-pair-val">中国人民大学（中关村校区）</span>
        </div>
        <div class="sp-pair">
          <span class="sp-pair-key">日常窗口</span>
          <span class="sp-pair-val">工作日 09:00 — 18:00（节假日邮件回复延迟）</span>
        </div>
        <div class="sp-pair">
          <span class="sp-pair-key">紧急通道</span>
          <span class="sp-pair-val">涉及账号安全 / 内容侵权，请在邮件标题前加「【紧急】」</span>
        </div>
      </div>
    </section>

    <section class="sp-card">
      <h2>怎么写邮件最有效</h2>
      <p>请从下面几条里挑一条匹配你的诉求，按建议格式发到 <a href="mailto:RUC_KAL@163.com">RUC_KAL@163.com</a>，我们会更快回复你：</p>
      <ul>
        <li>
          <strong>开通校外合作账号</strong>：标题《校外账号申请 · 项目名》。正文需含：合作者姓名 / 邮箱 / 与校内同学的关联（项目 ID 或邀请人学号）。
        </li>
        <li>
          <strong>账号 / 登录问题</strong>：标题《登录异常 · 你的邮箱前缀》。请说明：尝试操作 + 看到的错误提示 + 截图。<u>请勿在邮件中附带密码。</u>
        </li>
        <li>
          <strong>内容下线 / 申诉</strong>：标题《申诉 · 项目卡 ID 或帖子 ID》。请说明：被处置内容、你的解释、希望恢复或调整的方式。
        </li>
        <li>
          <strong>赛事信息收录</strong>：标题《赛事登记 · 比赛名》。请提供：比赛名 / 主办方 / 报名起止日期 / 官方链接 / 海报。
        </li>
        <li>
          <strong>合作 / 媒体</strong>：标题《合作 · 你的机构》。请说明合作类型、希望对接的范围与时间窗口。
        </li>
      </ul>
    </section>

    <section class="sp-card">
      <h2>我们处理诉求的时效</h2>
      <ul>
        <li>邮件回复：1–3 个工作日内首次响应。</li>
        <li>账号封禁申诉：5 个工作日内给出最终结论。</li>
        <li>内容下线申诉：3 个工作日内回复，必要时邀请第二位管理员复核。</li>
        <li>系统故障：收到说明 + 现场截图后，工作日内尽快定位修复；非工作日邮件回复有延迟。</li>
      </ul>
    </section>
  </StaticPage>
</template>

<style scoped>
.ct-copy {
  margin-left: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-size: var(--kal-text-2xs);
  letter-spacing: 1px;
  color: var(--ruc-red);
  background: var(--kal-primary-50);
  border: 1px solid var(--kal-primary-200);
  border-radius: var(--kal-radius-full);
  cursor: pointer;
  transition: all 180ms;
}
.ct-copy:hover {
  background: var(--kal-primary-100);
}
:deep(a) { word-break: break-all; }
</style>

<template>
  <main class="nexus-docs">
    <header class="docs-nav" :class="{ 'is-open': menuOpen }">
      <a class="brand" href="#top" aria-label="NexusEdu 首页" @click="closeMenu">
        <span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
        <span class="brand-word">NEXUS<span>EDU</span></span>
      </a>

      <button
        class="nav-toggle"
        type="button"
        :aria-expanded="menuOpen"
        aria-label="打开导航"
        @click="menuOpen = !menuOpen"
      >
        <span></span><span></span>
      </button>

      <nav class="nav-links" aria-label="项目介绍导航">
        <a href="#capabilities" @click="closeMenu">能力地图</a>
        <a href="#workflow" @click="closeMenu">AI 工作流</a>
        <a href="#architecture" @click="closeMenu">系统结构</a>
        <a href="#preview" @click="closeMenu">后台预览</a>
      </nav>

      <div class="nav-actions">
        <span class="locale-chip">ZH / 01</span>
        <RouterLink class="nav-cta" to="/dashboard" @click="closeMenu">
          进入后台 <span aria-hidden="true">↗</span>
        </RouterLink>
      </div>
    </header>

    <section id="top" class="hero shell">
      <div class="hero-copy reveal-up">
        <p class="eyebrow"><span class="pulse-dot"></span> AI LEARNING OPERATIONS / 2026</p>
        <h1>让教学数据，<em>开始协作。</em></h1>
        <p class="hero-lead">
          NexusEdu 把学生、知识、AI 任务和运行治理放进同一条可追踪的链路，
          为教学团队提供一个可以持续演进的管理后台。
        </p>
        <div class="hero-actions">
          <a class="button button-dark" href="#capabilities"
            >探索能力 <span aria-hidden="true">↓</span></a
          >
          <RouterLink class="button button-quiet" to="/dashboard"
            >打开管理后台 <span aria-hidden="true">↗</span></RouterLink
          >
        </div>
        <div class="hero-footnote">
          <span class="footnote-line"></span>
          <span>从一次提问，到一次可复盘的教学行动</span>
        </div>
      </div>

      <div class="hero-system" aria-label="NexusEdu 系统能力概览">
        <div class="system-orbit orbit-one"></div>
        <div class="system-orbit orbit-two"></div>
        <div class="system-core">
          <span class="core-kicker">NEXUS / CORE</span>
          <strong>TEACH<br /><i>BETTER</i></strong>
          <span class="core-status"><b></b> SYSTEM READY</span>
        </div>
        <div class="system-node node-a"
          ><span>01</span><strong>学生</strong><small>STUDENT</small></div
        >
        <div class="system-node node-b"
          ><span>02</span><strong>知识库</strong><small>KNOWLEDGE</small></div
        >
        <div class="system-node node-c"
          ><span>03</span><strong>AI 任务</strong><small>EXECUTION</small></div
        >
        <div class="system-node node-d"
          ><span>04</span><strong>治理</strong><small>OBSERVABILITY</small></div
        >
        <div class="system-caption">A CONNECTED<br />LEARNING SYSTEM</div>
      </div>
    </section>

    <section class="signal-strip" aria-label="平台特性">
      <div class="shell signal-inner">
        <span class="signal-label">NEXUSEDU / PLATFORM SIGNALS</span>
        <span v-for="signal in signals" :key="signal" class="signal-item"
          >{{ signal }} <b>↗</b></span
        >
      </div>
    </section>

    <section id="capabilities" class="section shell capabilities-section reveal-on-scroll">
      <div class="section-intro">
        <p class="section-index">01 / CAPABILITY MAP</p>
        <h2>把复杂的教学现场，<br /><span>拆成清晰的动作。</span></h2>
        <p>
          每一种能力都对应真实的业务入口。NexusEdu 让后台不只是数据列表，
          而是教学团队每天可以依赖的工作界面。
        </p>
      </div>

      <div class="capability-grid">
        <article
          v-for="(card, index) in capabilities"
          :key="card.title"
          class="capability-card reveal-on-scroll"
          :class="`tone-${index + 1}`"
          :style="{ '--reveal-delay': `${index * 70}ms` }"
        >
          <div class="card-topline"
            ><span>0{{ index + 1 }}</span
            ><span class="card-arrow">↗</span></div
          >
          <div class="card-icon" aria-hidden="true"><span v-for="n in 4" :key="n"></span></div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
          <div class="card-tags"
            ><span v-for="tag in card.tags" :key="tag">{{ tag }}</span></div
          >
        </article>
      </div>
    </section>

    <section id="workflow" class="workflow-section">
      <div class="shell workflow-layout">
        <div class="workflow-heading">
          <p class="section-index light">02 / AI WORKFLOW</p>
          <h2>一条可观察的<br /><i>AI 教学链路。</i></h2>
          <p>从上下文进入，到结果落地，过程中的每一步都可以被理解、被追踪、被改进。</p>
          <a class="text-link light-link" href="#preview">查看后台中的运行视图 <span>→</span></a>
        </div>
        <div class="workflow-list">
          <div
            v-for="(step, index) in workflow"
            :key="step.title"
            class="workflow-step reveal-on-scroll"
            :style="{ '--reveal-delay': `${index * 85}ms` }"
          >
            <span class="step-number">0{{ index + 1 }}</span>
            <div class="step-copy"
              ><h3>{{ step.title }}</h3
              ><p>{{ step.description }}</p></div
            >
            <span class="step-mark">{{ step.mark }}</span>
          </div>
        </div>
      </div>
    </section>

    <section id="architecture" class="section shell architecture-section">
      <div class="section-intro architecture-intro">
        <p class="section-index">03 / SYSTEM STRUCTURE</p>
        <h2>业务先行，<br /><span>边界自然长出来。</span></h2>
        <p>
          以 AI、知识、学生和系统为业务上下文，再用应用、领域和基础设施承接变化。
          代码结构服务于团队协作，也服务于下一次能力扩展。
        </p>
      </div>

      <div class="architecture-map" aria-label="NexusEdu 分层结构">
        <div class="map-header"><span>ARCHITECTURE / 04 LAYERS</span><span>LIVE MODEL</span></div>
        <div class="map-layers">
          <div class="map-layer layer-interface reveal-on-scroll"
            ><span class="layer-code">01</span><strong>INTERFACES</strong
            ><small>REST / EVENTS / WEB</small><b>↗</b></div
          >
          <div class="map-connector">↓</div>
          <div class="map-layer layer-application reveal-on-scroll"
            ><span class="layer-code">02</span><strong>APPLICATION</strong
            ><small>USE CASES / PORTS</small><b>↗</b></div
          >
          <div class="map-connector">↓</div>
          <div class="map-layer layer-domain reveal-on-scroll"
            ><span class="layer-code">03</span><strong>DOMAIN</strong><small>RULES / CONTEXTS</small
            ><b>↗</b></div
          >
          <div class="map-connector">↓</div>
          <div class="map-layer layer-infra reveal-on-scroll"
            ><span class="layer-code">04</span><strong>INFRASTRUCTURE</strong
            ><small>AI / DATA / OBSERVABILITY</small><b>↗</b></div
          >
        </div>
        <div class="map-footer"
          ><span>COHESION ↑</span><span>COUPLING ↓</span><span>CHANGE READY</span></div
        >
      </div>
    </section>

    <section id="preview" class="preview-section">
      <div class="shell preview-heading">
        <p class="section-index">04 / CONTROL ROOM</p>
        <div
          ><h2>后台，是每天真正要用的地方。</h2
          ><p>从菜单、角色到 AI 运行观测，NexusEdu 把复杂度收进清晰的操作路径里。</p></div
        >
        <RouterLink class="text-link" to="/dashboard">进入完整后台 <span>↗</span></RouterLink>
      </div>

      <div class="shell dashboard-frame reveal-on-scroll">
        <div class="frame-chrome"
          ><span class="chrome-dots"><i></i><i></i><i></i></span><span>nexusedu / console</span
          ><span>09:41:26 CST</span></div
        >
        <div class="frame-body">
          <aside class="mock-sidebar">
            <div class="mock-brand"
              ><span class="brand-mark small"><i></i><i></i><i></i></span
              ><strong>NEXUS<span>EDU</span></strong></div
            >
            <div class="mock-nav-label">WORKSPACE</div>
            <div class="mock-nav active"><b>⌂</b> 总览 <span>01</span></div>
            <div class="mock-nav"><b>◈</b> AI 运行</div>
            <div class="mock-nav"><b>▦</b> 知识库</div>
            <div class="mock-nav"><b>◎</b> 学生管理</div>
            <div class="mock-nav-label lower">SYSTEM</div>
            <div class="mock-nav"><b>◌</b> 观测与治理</div>
            <div class="mock-nav"><b>⚙</b> 系统设置</div>
            <div class="mock-user"
              ><span>XZ</span><div><strong>Xiaoyu Zhang</strong><small>Administrator</small></div
              ><b>···</b></div
            >
          </aside>
          <div class="mock-content">
            <div class="mock-top"
              ><div
                ><span class="mock-kicker">MONDAY / 19 OCT 2026</span
                ><h3>早上好，Xiaoyu <span>✦</span></h3></div
              ><div class="mock-top-actions"
                ><span>⌕</span><span>◔</span><span class="mock-avatar">XZ</span></div
              ></div
            >
            <div class="mock-grid">
              <div class="metric-card metric-primary"
                ><span>AI TASKS / THIS WEEK</span><strong>1,284</strong
                ><small><b>+18.4%</b> compared to last week</small
                ><div class="mini-chart"
                  ><i
                    v-for="n in 16"
                    :key="n"
                    :style="{ height: `${18 + ((n * 17) % 44)}px` }"
                  ></i></div
              ></div>
              <div class="metric-card"
                ><span>KNOWLEDGE RETRIEVAL</span><strong>92.8%</strong
                ><small><b class="mint">+4.2%</b> hit rate across sources</small
                ><div class="ring-chart"
                  ><i>92<span>%</span></i></div
                ></div
              >
              <div class="metric-card compact"
                ><span>ACTIVE STUDENTS</span><strong>3,096</strong
                ><small><b>+126</b> this month</small
                ><div class="avatar-stack"
                  ><span>AM</span><span>ZY</span><span>LW</span><span>+24</span></div
                ></div
              >
              <div class="run-card"
                ><div class="run-title"><span>LIVE AI RUNS</span><b>VIEW ALL ↗</b></div
                ><div v-for="run in runs" :key="run.name" class="run-row"
                  ><span class="run-status" :class="run.status"></span
                  ><div
                    ><strong>{{ run.name }}</strong
                    ><small>{{ run.detail }}</small></div
                  ><em>{{ run.time }}</em></div
                ></div
              >
              <div class="activity-card"
                ><div class="run-title"><span>ACTIVITY / 24H</span><b>EXPORT ↗</b></div
                ><div class="activity-chart"
                  ><span
                    v-for="n in 28"
                    :key="n"
                    :style="{ height: `${18 + ((n * 23) % 54)}%` }"
                  ></span></div
                ><div class="chart-axis"
                  ><span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span
                  ><span>NOW</span></div
                ></div
              >
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="closing-section shell">
      <div class="closing-mark" aria-hidden="true">N/</div>
      <p class="section-index">05 / START HERE</p>
      <h2>把下一次教学行动，<br /><em>交给一个更清晰的系统。</em></h2>
      <p>从项目介绍开始了解 NexusEdu，再进入后台，把你的业务上下文接进来。</p>
      <div class="hero-actions"
        ><RouterLink class="button button-dark" to="/dashboard"
          >进入 NexusEdu <span>↗</span></RouterLink
        ><a class="button button-outline" href="#top">回到顶部 <span>↑</span></a></div
      >
    </section>

    <footer class="docs-footer shell"
      ><div class="brand footer-brand"
        ><span class="brand-mark"><i></i><i></i><i></i></span
        ><span class="brand-word">NEXUS<span>EDU</span></span></div
      ><span>AI LEARNING OPERATIONS / 2026</span><span>nexusedu.cn/docs/zh</span></footer
    >
  </main>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue'
  import { RouterLink } from 'vue-router'

  const menuOpen = ref(false)
  let revealObserver: IntersectionObserver | null = null

  const signals = ['AI ASSISTANT', 'KNOWLEDGE GRAPH', 'STUDENT OPS', 'OBSERVABILITY']

  const capabilities = [
    {
      title: 'AI 工作台',
      description: '把对话、计划、工具调用和任务执行串成可复用的教学动作。',
      tags: ['CHAT', 'TASK RUN']
    },
    {
      title: '知识中枢',
      description: '文档、分块、检索和版本沉淀为一套可以持续更新的知识链路。',
      tags: ['RAG', 'INDEXING']
    },
    {
      title: '学生管理',
      description: '围绕学生档案与业务操作建立可靠的数据入口，支持团队协作。',
      tags: ['STUDENT', 'ACCESS']
    },
    {
      title: '运行治理',
      description: '观测每次模型调用、工具执行和评测结果，让 AI 能力可解释、可改进。',
      tags: ['METRICS', 'POLICY']
    }
  ]

  const workflow = [
    {
      title: '输入上下文',
      description: '学生、知识和业务规则被收进同一个任务上下文。',
      mark: 'CONTEXT'
    },
    {
      title: '规划行动',
      description: 'AI 将目标拆解为有边界的步骤，选择合适的工具和来源。',
      mark: 'PLAN'
    },
    {
      title: '执行任务',
      description: '每次调用都有状态、耗时、结果和异常，过程随时可见。',
      mark: 'RUN'
    },
    {
      title: '复盘改进',
      description: '用指标与评测结果回看质量，让下一次响应更接近教学目标。',
      mark: 'LEARN'
    }
  ]

  const runs = [
    {
      name: 'Lesson plan / Grade 8',
      detail: 'Knowledge retrieval · 4 steps',
      status: 'running',
      time: '00:08'
    },
    {
      name: 'Student summary / #2048',
      detail: 'Tool execution · complete',
      status: 'complete',
      time: '01:42'
    },
    {
      name: 'Reading feedback / Unit 03',
      detail: 'Evaluation pending',
      status: 'pending',
      time: '02:16'
    }
  ]

  const closeMenu = () => {
    menuOpen.value = false
  }

  onMounted(() => {
    document.body.classList.add('nexus-docs-page')

    if ('IntersectionObserver' in window) {
      revealObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('is-visible')
              revealObserver?.unobserve(entry.target)
            }
          })
        },
        { threshold: 0.12, rootMargin: '0px 0px -8% 0px' }
      )

      document.querySelectorAll('.nexus-docs .reveal-on-scroll').forEach((element) => {
        revealObserver?.observe(element)
      })
    } else {
      document.querySelectorAll('.nexus-docs .reveal-on-scroll').forEach((element) => {
        element.classList.add('is-visible')
      })
    }
  })

  onUnmounted(() => {
    revealObserver?.disconnect()
    revealObserver = null
    document.body.classList.remove('nexus-docs-page')
  })
</script>

<style lang="scss" scoped>
  .nexus-docs {
    --ink: #13252c;
    --ink-soft: #385159;
    --paper: #f4f3ed;
    --paper-deep: #e8e8df;
    --line: rgb(19 37 44 / 17%);
    --orange: #f36f3f;
    --mint: #b7e1d2;
    --yellow: #f2d36b;

    min-height: 100vh;
    overflow: hidden;
    font-family:
      Inter, 'Avenir Next', 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei UI', 'Helvetica Neue',
      sans-serif;
    color: var(--ink);
    background: var(--paper);
  }

  :global(body.nexus-docs-page) {
    background: var(--paper);
  }

  :global(body.nexus-docs-page #app) {
    min-height: 100vh;
  }

  :global(body.nexus-docs-page .el-loading-mask) {
    display: none;
  }

  .shell,
  .docs-nav {
    box-sizing: border-box;
    width: min(80%, 1500px);
    margin: 0 auto;
  }

  .shell {
    padding: 0;
  }

  .docs-nav {
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 78px;
    border-bottom: 1px solid var(--line);
  }

  .brand {
    display: inline-flex;
    gap: 11px;
    align-items: center;
    color: var(--ink);
    text-decoration: none;
  }

  .brand-word {
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0.16em;
  }

  .brand-word span {
    color: var(--orange);
  }

  .brand-mark {
    display: inline-flex;
    gap: 3px;
    align-items: end;
    width: 23px;
    height: 21px;
  }

  .brand-mark i {
    display: block;
    width: 5px;
    background: var(--orange);
    transform: skew(-21deg);
  }

  .brand-mark i:nth-child(1) {
    height: 12px;
  }

  .brand-mark i:nth-child(2) {
    height: 19px;
    background: var(--ink);
  }

  .brand-mark i:nth-child(3) {
    height: 15px;
    background: var(--yellow);
  }

  .nav-links {
    display: flex;
    gap: 30px;
    margin-right: 48px;
    margin-left: auto;
  }

  .nav-links a,
  .locale-chip {
    font-size: 12px;
    font-weight: 700;
    color: var(--ink-soft);
    text-decoration: none;
    letter-spacing: 0.04em;
    transition: color 180ms ease;
  }

  .nav-links a:hover {
    color: var(--orange);
  }

  .nav-actions {
    display: flex;
    gap: 22px;
    align-items: center;
  }

  .locale-chip {
    font-size: 10px;
    color: var(--ink);
    letter-spacing: 0.12em;
  }

  .nav-cta {
    display: inline-flex;
    gap: 12px;
    align-items: center;
    padding: 11px 14px;
    font-size: 11px;
    font-weight: 700;
    color: var(--paper);
    text-decoration: none;
    background: var(--ink);
    transition:
      background 180ms ease,
      transform 180ms ease;
  }

  .nav-cta:hover {
    background: var(--orange);
    transform: translateY(-2px);
  }

  .nav-toggle {
    display: none;
  }

  .hero {
    display: grid;
    grid-template-columns: minmax(0, 0.92fr) minmax(420px, 1.08fr);
    align-items: center;
    min-height: 650px;
    padding-top: 72px;
    padding-bottom: 98px;
  }

  .eyebrow,
  .section-index {
    margin: 0 0 25px;
    font-size: 10px;
    font-weight: 800;
    color: var(--orange);
    letter-spacing: 0.18em;
  }

  .pulse-dot {
    display: inline-block;
    width: 7px;
    height: 7px;
    margin-right: 8px;
    background: var(--orange);
    border-radius: 50%;
    box-shadow: 0 0 0 5px rgb(243 111 63 / 16%);
  }

  h1,
  h2,
  h3,
  p {
    margin-top: 0;
  }

  h1,
  h2,
  h3,
  .brand-word,
  .button,
  .nav-cta {
    font-family:
      Inter, 'Avenir Next', 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei UI', 'Helvetica Neue',
      sans-serif;
    font-feature-settings: 'kern', 'liga';
  }

  h1 {
    max-width: 640px;
    margin-bottom: 25px;
    font-size: clamp(52px, 6.5vw, 92px);
    font-weight: 750;
    line-height: 0.98;
    letter-spacing: -0.045em;
  }

  h1 em,
  h2 em {
    font-family: inherit;
    font-style: normal;
    font-weight: 700;
    color: var(--orange);
    letter-spacing: inherit;
  }

  .hero-lead {
    max-width: 475px;
    margin-bottom: 34px;
    font-size: 16px;
    line-height: 1.85;
    color: var(--ink-soft);
  }

  .hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
  }

  .button {
    display: inline-flex;
    align-items: center;
    justify-content: space-between;
    min-width: 154px;
    padding: 14px 16px;
    font-size: 12px;
    font-weight: 800;
    text-decoration: none;
    letter-spacing: 0.03em;
    border: 1px solid var(--ink);
    transition:
      transform 180ms ease,
      background 180ms ease,
      color 180ms ease;
  }

  .button span {
    margin-left: 18px;
    font-size: 16px;
  }

  .button:hover {
    transform: translateY(-3px);
  }

  .button-dark {
    color: var(--paper);
    background: var(--ink);
  }

  .button-dark:hover {
    background: var(--orange);
    border-color: var(--orange);
  }

  .button-quiet {
    color: var(--ink);
    border-color: transparent;
  }

  .button-quiet:hover {
    color: var(--orange);
  }

  .button-outline {
    color: var(--ink);
    background: transparent;
  }

  .button-outline:hover {
    color: var(--paper);
    background: var(--ink);
  }

  .hero-footnote {
    display: flex;
    gap: 13px;
    align-items: center;
    margin-top: 68px;
    font-size: 11px;
    color: #758184;
  }

  .footnote-line {
    width: 40px;
    height: 1px;
    background: var(--orange);
  }

  .hero-system {
    position: relative;
    width: min(600px, 100%);
    height: 505px;
    margin-left: auto;
  }

  .hero-system::before {
    position: absolute;
    inset: 34px 20px 14px 30px;
    content: '';
    background-image:
      linear-gradient(rgb(19 37 44 / 9%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(19 37 44 / 9%) 1px, transparent 1px);
    background-size: 36px 36px;
    transform: perspective(600px) rotateX(56deg) rotateZ(-25deg) scale(1.14);
    transform-origin: center;
  }

  .hero-system::after {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 170px;
    height: 170px;
    content: '';
    border: 1px solid rgb(243 111 63 / 60%);
    border-radius: 50%;
  }

  .system-orbit {
    position: absolute;
    border: 1px solid rgb(19 37 44 / 22%);
    border-radius: 50%;
    transform: rotate(-26deg);
  }

  .orbit-one {
    top: 56px;
    left: 68px;
    width: 430px;
    height: 330px;
  }

  .orbit-two {
    top: 89px;
    left: 112px;
    width: 355px;
    height: 275px;
    border-color: rgb(243 111 63 / 38%);
    transform: rotate(32deg);
  }

  .system-core {
    position: absolute;
    top: 150px;
    left: 199px;
    z-index: 2;
    display: flex;
    flex-direction: column;
    justify-content: center;
    width: 190px;
    height: 190px;
    padding: 27px;
    color: var(--paper);
    background: var(--ink);
    border-radius: 50%;
    box-shadow: 18px 22px 0 rgb(243 111 63 / 78%);
    transform: rotate(-8deg);
  }

  .core-kicker,
  .core-status {
    font-size: 8px;
    letter-spacing: 0.16em;
  }

  .core-kicker {
    color: var(--mint);
  }

  .system-core strong {
    margin: 13px 0 15px;
    font-size: 29px;
    line-height: 0.83;
    letter-spacing: -0.035em;
  }

  .system-core strong i {
    font-family: inherit;
    font-style: normal;
    font-weight: 650;
    color: var(--yellow);
  }

  .core-status {
    display: flex;
    gap: 6px;
    align-items: center;
    color: #a8b6b5;
  }

  .core-status b {
    width: 5px;
    height: 5px;
    background: var(--mint);
    border-radius: 50%;
  }

  .system-node {
    position: absolute;
    z-index: 3;
    display: flex;
    flex-direction: column;
    gap: 4px;
    width: 128px;
    padding: 12px 14px;
    background: rgb(244 243 237 / 90%);
    border: 1px solid var(--line);
    box-shadow: 6px 7px 0 rgb(19 37 44 / 8%);
  }

  .system-node span,
  .system-node small {
    font-size: 9px;
    color: var(--orange);
    letter-spacing: 0.1em;
  }

  .system-node strong {
    font-size: 14px;
  }

  .system-node small {
    font-size: 8px;
    color: #7b898a;
  }

  .node-a {
    top: 56px;
    left: 8px;
  }

  .node-b {
    top: 85px;
    right: 1px;
  }

  .node-c {
    bottom: 50px;
    left: 55px;
  }

  .node-d {
    right: 39px;
    bottom: 25px;
  }

  .system-caption {
    position: absolute;
    top: 29px;
    right: 60px;
    font-size: 9px;
    font-weight: 800;
    line-height: 1.5;
    color: var(--orange);
    text-align: right;
    letter-spacing: 0.12em;
  }

  .signal-strip {
    background: var(--paper-deep);
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
  }

  .signal-inner {
    display: flex;
    align-items: center;
    min-height: 54px;
    overflow: hidden;
  }

  .signal-label {
    margin-right: auto;
    font-size: 9px;
    font-weight: 800;
    color: #7a8584;
    letter-spacing: 0.16em;
    white-space: nowrap;
  }

  .signal-item {
    display: inline-flex;
    gap: 14px;
    align-items: center;
    margin-left: 40px;
    font-size: 10px;
    font-weight: 800;
    color: var(--ink);
    letter-spacing: 0.08em;
    white-space: nowrap;
  }

  .signal-item b {
    font-size: 15px;
    color: var(--orange);
  }

  .section {
    padding-top: 150px;
    padding-bottom: 150px;
  }

  .section-intro {
    display: grid;
    grid-template-columns: 0.9fr 1.5fr 1fr;
    gap: 40px;
    align-items: start;
    margin-bottom: 65px;
  }

  .section-index {
    margin: 8px 0 0;
  }

  .section-intro h2,
  .workflow-heading h2,
  .closing-section h2 {
    margin: 0;
    font-size: clamp(38px, 4.6vw, 65px);
    font-weight: 740;
    line-height: 1.02;
    letter-spacing: -0.04em;
  }

  .section-intro h2 span {
    font-family: inherit;
    font-style: normal;
    font-weight: 650;
    color: var(--orange);
  }

  .section-intro > p:last-child {
    max-width: 270px;
    margin: 7px 0 0;
    font-size: 14px;
    line-height: 1.8;
    color: var(--ink-soft);
  }

  .capability-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
  }

  .capability-card {
    position: relative;
    min-height: 340px;
    padding: 20px;
    border: 1px solid var(--line);
    transition:
      transform 220ms ease,
      box-shadow 220ms ease;
  }

  .capability-card:hover {
    box-shadow: 10px 12px 0 rgb(19 37 44 / 12%);
    transform: translateY(-7px);
  }

  .tone-1 {
    background: var(--mint);
  }

  .tone-2 {
    background: #dfdfcf;
  }

  .tone-3 {
    color: var(--paper);
    background: var(--ink);
  }

  .tone-4 {
    background: var(--yellow);
  }

  .card-topline {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    font-weight: 800;
    color: rgb(19 37 44 / 62%);
    letter-spacing: 0.13em;
  }

  .tone-3 .card-topline {
    color: rgb(244 243 237 / 64%);
  }

  .card-arrow {
    font-size: 17px;
  }

  .card-icon {
    display: grid;
    grid-template-columns: repeat(2, 11px);
    gap: 4px;
    margin: 58px 0 36px;
  }

  .card-icon span {
    width: 11px;
    height: 11px;
    border: 1px solid var(--ink);
  }

  .card-icon span:nth-child(2) {
    background: var(--orange);
    border-color: var(--orange);
  }

  .card-icon span:nth-child(3) {
    background: var(--ink);
  }

  .tone-3 .card-icon span {
    border-color: var(--paper);
  }

  .tone-3 .card-icon span:nth-child(2) {
    background: var(--yellow);
    border-color: var(--yellow);
  }

  .tone-3 .card-icon span:nth-child(3) {
    background: var(--mint);
    border-color: var(--mint);
  }

  .capability-card h3 {
    margin-bottom: 11px;
    font-size: 22px;
    letter-spacing: -0.025em;
  }

  .capability-card p {
    min-height: 69px;
    margin-bottom: 24px;
    font-size: 12px;
    line-height: 1.7;
    color: var(--ink-soft);
  }

  .tone-3 p {
    color: #b9c7c5;
  }

  .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .card-tags span {
    padding: 5px 6px;
    font-size: 8px;
    font-weight: 800;
    color: var(--ink-soft);
    letter-spacing: 0.08em;
    border: 1px solid rgb(19 37 44 / 28%);
  }

  .tone-3 .card-tags span {
    color: var(--paper-deep);
    border-color: rgb(244 243 237 / 35%);
  }

  .workflow-section {
    color: var(--paper);
    background: var(--ink);
  }

  .workflow-layout {
    display: grid;
    grid-template-columns: 0.8fr 1.2fr;
    gap: 130px;
    padding-top: 145px;
    padding-bottom: 145px;
  }

  .section-index.light {
    color: var(--yellow);
  }

  .workflow-heading h2 i {
    font-family: inherit;
    font-style: normal;
    font-weight: 650;
    color: var(--mint);
  }

  .workflow-heading > p:not(.section-index) {
    max-width: 305px;
    margin: 30px 0 38px;
    font-size: 14px;
    line-height: 1.8;
    color: #abbab8;
  }

  .text-link {
    font-size: 12px;
    font-weight: 800;
    color: var(--ink);
    text-decoration: none;
  }

  .text-link span {
    margin-left: 10px;
    font-size: 16px;
    color: var(--orange);
  }

  .light-link {
    color: var(--paper);
  }

  .workflow-list {
    border-top: 1px solid rgb(244 243 237 / 24%);
  }

  .workflow-step {
    display: grid;
    grid-template-columns: 58px 1fr auto;
    gap: 22px;
    align-items: center;
    min-height: 123px;
    border-bottom: 1px solid rgb(244 243 237 / 24%);
  }

  .step-number {
    align-self: start;
    padding-top: 25px;
    font-size: 11px;
    font-weight: 800;
    color: var(--orange);
    letter-spacing: 0.1em;
  }

  .step-copy h3 {
    margin-bottom: 8px;
    font-size: 20px;
    letter-spacing: -0.04em;
  }

  .step-copy p {
    margin: 0;
    font-size: 12px;
    line-height: 1.6;
    color: #9cacab;
  }

  .step-mark {
    font-size: 9px;
    font-weight: 800;
    color: var(--mint);
    letter-spacing: 0.16em;
  }

  .architecture-section {
    display: grid;
    grid-template-columns: 0.82fr 1.18fr;
    gap: 120px;
    align-items: center;
  }

  .architecture-intro {
    display: block;
    margin-bottom: 0;
  }

  .architecture-intro h2 {
    margin-bottom: 30px;
  }

  .architecture-intro > p:last-child {
    max-width: 315px;
  }

  .architecture-map {
    padding: 24px;
    background: #e6e7de;
    border: 1px solid var(--line);
  }

  .map-header,
  .map-footer {
    display: flex;
    justify-content: space-between;
    font-size: 8px;
    font-weight: 800;
    color: #788486;
    letter-spacing: 0.14em;
  }

  .map-layers {
    padding: 38px 20px 35px;
  }

  .map-layer {
    display: grid;
    grid-template-columns: 42px 1fr 1fr 30px;
    align-items: center;
    min-height: 62px;
    padding: 0 16px;
    border: 1px solid var(--ink);
  }

  .map-layer strong {
    font-size: 13px;
    letter-spacing: 0.08em;
  }

  .map-layer small,
  .layer-code {
    font-size: 8px;
    font-weight: 800;
    color: #647577;
    letter-spacing: 0.1em;
  }

  .map-layer b {
    justify-self: end;
    font-size: 17px;
    color: var(--orange);
  }

  .layer-interface {
    background: var(--paper);
  }

  .layer-application {
    color: var(--paper);
    background: var(--ink);
  }

  .layer-application .layer-code,
  .layer-application small {
    color: #9fb0ae;
  }

  .layer-domain {
    background: var(--mint);
  }

  .layer-infra {
    background: var(--yellow);
  }

  .map-connector {
    height: 21px;
    font-size: 15px;
    line-height: 21px;
    color: var(--orange);
    text-align: center;
  }

  .map-footer {
    padding-top: 15px;
    border-top: 1px solid rgb(19 37 44 / 15%);
  }

  .preview-section {
    padding: 140px 0 150px;
    color: var(--paper);
    background: #172a31;
  }

  .preview-heading {
    display: grid;
    grid-template-columns: 0.8fr 1.4fr auto;
    gap: 50px;
    align-items: end;
    margin-bottom: 52px;
  }

  .preview-heading h2 {
    margin: 0 0 12px;
    font-family: inherit;
    font-size: clamp(36px, 4vw, 57px);
    font-style: normal;
    line-height: 1.12;
    letter-spacing: -0.035em;
  }

  .preview-heading p:not(.section-index) {
    max-width: 390px;
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: #aab9b7;
  }

  .preview-heading .text-link {
    color: var(--paper);
    white-space: nowrap;
  }

  .dashboard-frame {
    background: #dfe2dc;
    border: 1px solid rgb(244 243 237 / 35%);
    box-shadow: 18px 18px 0 rgb(243 111 63 / 72%);
  }

  .frame-chrome {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    align-items: center;
    min-height: 38px;
    padding: 0 16px;
    font-size: 8px;
    font-weight: 800;
    color: #768481;
    text-align: center;
    letter-spacing: 0.14em;
    background: #eef0e9;
  }

  .frame-chrome > span:last-child {
    text-align: right;
  }

  .chrome-dots {
    display: flex;
    gap: 5px;
    text-align: left;
  }

  .chrome-dots i {
    width: 7px;
    height: 7px;
    background: #a9b6b0;
    border-radius: 50%;
  }

  .chrome-dots i:first-child {
    background: var(--orange);
  }

  .frame-body {
    display: grid;
    grid-template-columns: 190px 1fr;
    min-height: 500px;
  }

  .mock-sidebar {
    padding: 23px 13px;
    color: #b2c0be;
    background: #172a31;
  }

  .mock-brand {
    display: flex;
    gap: 8px;
    align-items: center;
    margin: 0 10px 50px;
    font-size: 10px;
    color: var(--paper);
    letter-spacing: 0.13em;
  }

  .mock-brand strong span {
    color: var(--orange);
  }

  .brand-mark.small {
    gap: 2px;
    width: 17px;
    height: 16px;
  }

  .brand-mark.small i {
    width: 4px;
  }

  .brand-mark.small i:nth-child(1) {
    height: 9px;
  }

  .brand-mark.small i:nth-child(2) {
    height: 14px;
  }

  .brand-mark.small i:nth-child(3) {
    height: 11px;
  }

  .mock-nav-label {
    margin: 0 10px 13px;
    font-size: 8px;
    font-weight: 800;
    color: #718584;
    letter-spacing: 0.15em;
  }

  .mock-nav-label.lower {
    margin-top: 36px;
  }

  .mock-nav {
    display: flex;
    gap: 11px;
    align-items: center;
    padding: 10px;
    font-size: 10px;
    color: #a9b8b5;
  }

  .mock-nav b {
    width: 13px;
    font-size: 13px;
    color: #7d9793;
    text-align: center;
  }

  .mock-nav span {
    margin-left: auto;
    font-size: 9px;
    color: var(--orange);
  }

  .mock-nav.active {
    color: var(--paper);
    background: #2d484a;
    border-left: 2px solid var(--orange);
  }

  .mock-user {
    display: flex;
    gap: 8px;
    align-items: center;
    padding-top: 15px;
    margin: 60px 5px 0;
    border-top: 1px solid rgb(244 243 237 / 13%);
  }

  .mock-user > span,
  .mock-avatar {
    display: grid;
    place-items: center;
    width: 27px;
    height: 27px;
    font-size: 8px;
    font-weight: 800;
    color: var(--ink);
    background: var(--yellow);
    border-radius: 50%;
  }

  .mock-user strong,
  .mock-user small {
    display: block;
    font-size: 8px;
  }

  .mock-user small {
    margin-top: 3px;
    font-size: 7px;
    color: #718584;
  }

  .mock-user > b {
    margin-left: auto;
    color: #718584;
  }

  .mock-content {
    padding: 27px 32px;
    color: var(--ink);
    background: #f7f7f1;
  }

  .mock-top {
    display: flex;
    align-items: start;
    justify-content: space-between;
    margin-bottom: 25px;
  }

  .mock-kicker {
    font-size: 8px;
    font-weight: 800;
    color: #899593;
    letter-spacing: 0.14em;
  }

  .mock-top h3 {
    margin: 9px 0 0;
    font-size: 21px;
    letter-spacing: -0.06em;
  }

  .mock-top h3 span {
    font-size: 16px;
    color: var(--orange);
  }

  .mock-top-actions {
    display: flex;
    gap: 14px;
    align-items: center;
    font-size: 15px;
    color: #788486;
  }

  .mock-avatar {
    width: 28px;
    height: 28px;
    margin-left: 3px;
  }

  .mock-grid {
    display: grid;
    grid-template-columns: 1.15fr 0.95fr 0.95fr;
    gap: 11px;
  }

  .metric-card,
  .run-card,
  .activity-card {
    position: relative;
    min-height: 151px;
    padding: 16px;
    background: #fffefa;
    border: 1px solid #d8dcd3;
  }

  .metric-card > span,
  .run-title span {
    font-size: 8px;
    font-weight: 800;
    color: #7d8987;
    letter-spacing: 0.13em;
  }

  .metric-card strong {
    display: block;
    margin: 14px 0 7px;
    font-size: 31px;
    letter-spacing: -0.07em;
  }

  .metric-card small {
    font-size: 8px;
    color: #8a9592;
  }

  .metric-card small b {
    margin-right: 4px;
    color: var(--orange);
  }

  .metric-card small b.mint {
    color: #3b9f81;
  }

  .metric-primary {
    overflow: hidden;
    color: var(--paper);
    background: var(--ink);
    border-color: var(--ink);
  }

  .metric-primary > span {
    color: #a6bab5;
  }

  .metric-primary small {
    color: #a6bab5;
  }

  .mini-chart {
    position: absolute;
    right: 13px;
    bottom: 14px;
    display: flex;
    gap: 3px;
    align-items: end;
    height: 52px;
  }

  .mini-chart i {
    display: block;
    width: 5px;
    background: var(--orange);
    opacity: 0.8;
  }

  .ring-chart {
    position: absolute;
    right: 18px;
    bottom: 13px;
    display: grid;
    place-items: center;
    width: 59px;
    height: 59px;
    border: 6px solid var(--mint);
    border-left-color: #e4e8de;
    border-radius: 50%;
  }

  .ring-chart i {
    font-size: 15px;
    font-style: normal;
    font-weight: 800;
    color: var(--ink);
  }

  .ring-chart i span {
    font-size: 8px;
  }

  .compact {
    grid-column: 3;
  }

  .avatar-stack {
    display: flex;
    margin-top: 17px;
  }

  .avatar-stack span {
    display: grid;
    place-items: center;
    width: 24px;
    height: 24px;
    margin-right: -5px;
    font-size: 7px;
    font-weight: 800;
    color: var(--ink);
    background: var(--mint);
    border: 2px solid #fffefa;
    border-radius: 50%;
  }

  .avatar-stack span:nth-child(2) {
    background: var(--yellow);
  }

  .avatar-stack span:nth-child(3) {
    background: #f39c81;
  }

  .avatar-stack span:last-child {
    color: #748380;
    background: #e5e8df;
  }

  .run-card {
    grid-column: span 1;
    min-height: 213px;
  }

  .run-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .run-title b {
    font-size: 7px;
    color: var(--orange);
    letter-spacing: 0.08em;
  }

  .run-row {
    display: grid;
    grid-template-columns: 10px 1fr auto;
    gap: 8px;
    align-items: center;
    padding: 13px 0;
    border-top: 1px solid #e3e6de;
  }

  .run-status {
    width: 6px;
    height: 6px;
    background: var(--orange);
    border-radius: 50%;
    box-shadow: 0 0 0 4px rgb(243 111 63 / 15%);
  }

  .run-status.complete {
    background: #48a986;
    box-shadow: 0 0 0 4px rgb(72 169 134 / 14%);
  }

  .run-status.pending {
    background: #c1a53d;
    box-shadow: 0 0 0 4px rgb(193 165 61 / 14%);
  }

  .run-row strong,
  .run-row small {
    display: block;
  }

  .run-row strong {
    font-size: 9px;
  }

  .run-row small {
    margin-top: 3px;
    font-size: 7px;
    color: #899593;
  }

  .run-row em {
    font-size: 8px;
    font-style: normal;
    color: #899593;
  }

  .activity-card {
    grid-column: span 2;
    min-height: 213px;
  }

  .activity-chart {
    display: flex;
    gap: 6px;
    align-items: end;
    height: 126px;
    padding: 17px 8px 5px;
    border-bottom: 1px solid #d8dcd3;
  }

  .activity-chart span {
    flex: 1;
    min-width: 3px;
    background: var(--mint);
  }

  .activity-chart span:nth-child(3n) {
    background: var(--orange);
  }

  .chart-axis {
    display: flex;
    justify-content: space-between;
    padding: 8px 7px 0;
    font-size: 7px;
    color: #9aa5a1;
  }

  .closing-section {
    position: relative;
    padding-top: 160px;
    padding-bottom: 145px;
  }

  .closing-mark {
    position: absolute;
    top: 95px;
    right: 2%;
    font-size: 130px;
    font-weight: 900;
    color: var(--mint);
    letter-spacing: -0.18em;
    opacity: 0.58;
  }

  .closing-section h2 {
    max-width: 760px;
    margin-bottom: 25px;
    letter-spacing: -0.04em;
  }

  .closing-section h2 em {
    font-style: normal;
    font-weight: 650;
  }

  .closing-section > p:not(.section-index) {
    max-width: 380px;
    margin-bottom: 34px;
    font-size: 14px;
    line-height: 1.8;
    color: var(--ink-soft);
  }

  .docs-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 90px;
    font-size: 9px;
    font-weight: 800;
    color: #7c8888;
    letter-spacing: 0.12em;
    border-top: 1px solid var(--line);
  }

  .footer-brand {
    color: var(--ink);
  }

  // 与后台页面共享 Art Design Pro 的浅色、蓝色、卡片化视觉语言。
  /* stylelint-disable no-duplicate-selectors */
  .nexus-docs {
    --ink: #273449;
    --ink-soft: #77859a;
    --paper: #f6f8fc;
    --paper-deep: #edf2fa;
    --line: #e5eaf2;
    --orange: var(--theme-color, #5d87ff);
    --mint: #edf3ff;
    --yellow: #f4f7ff;
    --card: #fff;
    --accent-teal: #10a989;
    --accent-violet: #8065d8;
    --accent-amber: #e49a32;
    --tint-teal: #eaf8f5;
    --tint-violet: #f2efff;
    --tint-amber: #fff6e7;
  }

  .docs-nav {
    background: rgb(255 255 255 / 86%);
  }

  .brand-mark i {
    background: var(--orange);
  }

  .brand-mark i:nth-child(2) {
    background: #8eacff;
  }

  .brand-mark i:nth-child(3) {
    background: #c8d6ff;
  }

  .nav-cta,
  .button-dark {
    background: var(--orange);
    border-radius: 8px;
  }

  .nav-cta:hover,
  .button-dark:hover {
    background: #416fe8;
    border-color: #416fe8;
  }

  .button,
  .button-outline {
    border-radius: 8px;
  }

  .button {
    border-color: #d7e1f5;
  }

  .button-quiet {
    color: var(--ink-soft);
  }

  .button-outline {
    border-color: #ccd8ee;
  }

  .button-outline:hover {
    color: var(--orange);
    background: var(--mint);
  }

  .hero-system::before {
    background-image:
      linear-gradient(rgb(93 135 255 / 9%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(93 135 255 / 9%) 1px, transparent 1px);
  }

  .hero-system::after {
    width: 116px;
    height: 116px;
    border-color: rgb(93 135 255 / 22%);
    border-radius: 14px;
  }

  .system-orbit {
    border-color: rgb(93 135 255 / 22%);
  }

  .orbit-two {
    border-color: rgb(128 101 216 / 32%);
    border-style: dashed;
  }

  .system-core {
    color: var(--ink);
    background: var(--card);
    border: 1px solid #dfe7f4;
    box-shadow: 18px 22px 0 rgb(93 135 255 / 14%);
  }

  .core-kicker,
  .core-status {
    color: var(--orange);
  }

  .system-core strong i {
    color: var(--orange);
  }

  .core-status b {
    background: #13deb9;
  }

  .system-node {
    background: rgb(255 255 255 / 94%);
    border-color: var(--line);
    border-radius: 8px;
    box-shadow: 0 8px 20px rgb(39 52 73 / 8%);
  }

  .signal-strip {
    background: var(--card);
  }

  .capability-card {
    background: var(--card);
    border-radius: 12px;
    box-shadow: 0 4px 16px rgb(39 52 73 / 4%);
  }

  .capability-card:hover {
    box-shadow: 0 12px 28px rgb(39 52 73 / 10%);
  }

  .tone-1,
  .tone-2,
  .tone-3,
  .tone-4 {
    color: var(--ink);
    background: var(--card);
  }

  .tone-1 {
    background: #eff4ff;
  }

  .tone-2 {
    background: var(--tint-teal);
  }

  .tone-3 {
    background: var(--tint-violet);
  }

  .tone-4 {
    background: var(--tint-amber);
  }

  .tone-3 .card-topline,
  .tone-3 p {
    color: var(--ink-soft);
  }

  .tone-3 .card-icon span {
    border-color: var(--ink);
  }

  .tone-3 .card-icon span:nth-child(2) {
    background: var(--orange);
    border-color: var(--orange);
  }

  .tone-3 .card-icon span:nth-child(3) {
    background: #a9c0ff;
    border-color: #a9c0ff;
  }

  .card-tags span {
    color: var(--ink-soft);
    border-color: #dce5f5;
    border-radius: 4px;
  }

  .tone-3 .card-tags span {
    color: var(--ink-soft);
    border-color: #dce5f5;
  }

  .workflow-section {
    color: var(--ink);
    background: linear-gradient(115deg, #eef4ff 0%, #f4f1ff 52%, #edf8f7 100%);
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
  }

  .section-index.light,
  .workflow-heading h2 i,
  .step-number,
  .step-mark {
    color: var(--orange);
  }

  .workflow-heading h2 i {
    font-style: normal;
    font-weight: 650;
  }

  .workflow-section {
    background: linear-gradient(115deg, #eef4ff 0%, #f4f1ff 52%, #edf8f7 100%);
  }

  .workflow-heading > p:not(.section-index),
  .step-copy p {
    color: var(--ink-soft);
  }

  .light-link {
    color: var(--ink);
  }

  .workflow-list,
  .workflow-step {
    border-color: var(--line);
  }

  .architecture-map {
    background: var(--card);
    border-radius: 12px;
    box-shadow: 0 4px 16px rgb(39 52 73 / 4%);
  }

  .map-layer {
    border-color: #d8e1f0;
    border-radius: 8px;
  }

  .layer-interface,
  .layer-domain,
  .layer-infra,
  .layer-application {
    color: var(--ink);
    background: var(--card);
  }

  .layer-application {
    background: var(--mint);
  }

  .layer-domain {
    background: var(--tint-teal);
  }

  .layer-infra {
    background: var(--tint-amber);
  }

  .layer-application .layer-code,
  .layer-application small {
    color: var(--ink-soft);
  }

  .layer-domain {
    background: var(--tint-teal);
  }

  .layer-infra {
    background: var(--tint-amber);
  }

  .map-footer {
    border-color: var(--line);
  }

  .preview-section {
    color: var(--ink);
    background: var(--paper);
  }

  .preview-heading p:not(.section-index),
  .preview-heading .text-link {
    color: var(--ink-soft);
  }

  .dashboard-frame {
    overflow: hidden;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 14px;
    box-shadow: 0 12px 30px rgb(39 52 73 / 8%);
  }

  .frame-chrome {
    background: var(--card);
    border-bottom: 1px solid var(--line);
  }

  .chrome-dots i {
    background: #cbd5e5;
  }

  .chrome-dots i:first-child {
    background: var(--orange);
  }

  .mock-sidebar {
    color: var(--ink-soft);
    background: var(--card);
    border-right: 1px solid var(--line);
  }

  .mock-brand {
    color: var(--ink);
  }

  .mock-nav {
    color: var(--ink-soft);
    border-radius: 7px;
  }

  .mock-nav b {
    color: #94a3b8;
  }

  .mock-nav.active {
    color: var(--orange);
    background: var(--mint);
    border-left-color: var(--orange);
  }

  .mock-user {
    border-color: var(--line);
  }

  .mock-content {
    background: #f8fafc;
  }

  .metric-card,
  .run-card,
  .activity-card {
    background: var(--card);
    border-color: var(--line);
    border-radius: 10px;
  }

  .metric-primary {
    color: var(--ink);
    background: #eef3ff;
    border-color: #dce6ff;
  }

  .metric-card:nth-child(2) {
    background: var(--tint-teal);
    border-color: #d6eee8;
  }

  .metric-card.compact {
    background: var(--tint-violet);
    border-color: #e5defb;
  }

  .metric-card:nth-child(2) > span,
  .metric-card:nth-child(2) small b {
    color: var(--accent-teal);
  }

  .metric-card.compact > span,
  .metric-card.compact small b {
    color: var(--accent-violet);
  }

  .run-status.complete {
    background: var(--accent-teal);
  }

  .run-status.pending {
    background: var(--accent-amber);
  }

  .metric-primary > span,
  .metric-primary small {
    color: var(--ink-soft);
  }

  .metric-primary small b {
    color: #13b98d;
  }

  .closing-section {
    background: linear-gradient(135deg, #fff 0%, #f4f7ff 58%, #f7f3ff 100%);
  }

  .closing-mark {
    color: #e5edff;
  }

  .docs-footer {
    background: var(--card);
  }

  /* stylelint-enable no-duplicate-selectors */

  @keyframes reveal-up {
    from {
      opacity: 0;
      transform: translateY(18px);
    }

    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .reveal-up {
    animation: reveal-up 700ms ease both;
  }

  @keyframes reveal-on-scroll {
    from {
      opacity: 0;
      translate: 0 22px;
    }

    to {
      opacity: 1;
      translate: 0 0;
    }
  }

  @keyframes node-float {
    0%,
    100% {
      transform: translateY(0);
    }

    50% {
      transform: translateY(-7px);
    }
  }

  @keyframes orbit-breathe {
    0%,
    100% {
      opacity: 0.68;
    }

    50% {
      opacity: 1;
    }
  }

  @keyframes bar-pulse {
    0%,
    100% {
      transform: scaleY(0.72);
    }

    50% {
      transform: scaleY(1);
    }
  }

  @keyframes ring-pop {
    from {
      opacity: 0.72;
      transform: scale(0.94);
    }

    to {
      opacity: 1;
      transform: scale(1);
    }
  }

  .reveal-on-scroll {
    opacity: 0;
    translate: 0 22px;
  }

  .reveal-on-scroll.is-visible {
    animation: reveal-on-scroll 650ms cubic-bezier(0.22, 1, 0.36, 1) var(--reveal-delay, 0ms) both;
  }

  /* stylelint-disable no-duplicate-selectors */
  .pulse-dot {
    animation: orbit-breathe 2.8s ease-in-out infinite;
  }

  .system-node {
    animation: node-float 5.5s ease-in-out infinite;
  }

  .node-b {
    animation-delay: -1.4s;
  }

  .node-c {
    animation-delay: -2.8s;
  }

  .node-d {
    animation-delay: -4.1s;
  }

  .system-orbit {
    animation: orbit-breathe 6s ease-in-out infinite;
  }

  .orbit-two {
    animation-delay: -2.5s;
  }

  .mini-chart i,
  .activity-chart span {
    transform-origin: bottom;
    animation: bar-pulse 3.6s ease-in-out infinite;
  }

  .mini-chart i:nth-child(3n),
  .activity-chart span:nth-child(4n) {
    animation-delay: -1.1s;
  }

  .mini-chart i:nth-child(3n + 1),
  .activity-chart span:nth-child(4n + 1) {
    animation-delay: -2.2s;
  }

  .ring-chart {
    animation: ring-pop 2.4s ease-in-out infinite alternate;
  }
  /* stylelint-enable no-duplicate-selectors */

  @media (width <= 1050px) {
    .nav-links {
      gap: 17px;
      margin-right: 25px;
    }

    .hero {
      grid-template-columns: 1fr 0.9fr;
      gap: 25px;
    }

    .hero-system {
      transform: scale(0.85);
      transform-origin: center right;
    }

    .section-intro,
    .preview-heading {
      grid-template-columns: 0.7fr 1.3fr;
    }

    .section-intro > p:last-child {
      grid-column: 2;
    }

    .capability-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .architecture-section {
      gap: 55px;
    }

    .workflow-layout {
      gap: 60px;
    }
  }

  @media (width <= 720px) {
    .docs-nav {
      position: relative;
      height: 70px;
    }

    .nav-toggle {
      display: flex;
      flex-direction: column;
      gap: 5px;
      padding: 9px;
      background: transparent;
      border: 0;
    }

    .nav-toggle span {
      display: block;
      width: 23px;
      height: 1px;
      background: var(--ink);
    }

    .nav-links {
      position: absolute;
      top: 69px;
      right: 0;
      left: 0;
      display: none;
      flex-direction: column;
      gap: 0;
      padding: 8px 18px;
      margin: 0;
      background: var(--paper);
      border: 1px solid var(--line);
      box-shadow: 8px 8px 0 rgb(19 37 44 / 10%);
    }

    .is-open .nav-links {
      display: flex;
    }

    .nav-links a {
      padding: 13px 0;
      border-bottom: 1px solid var(--line);
    }

    .nav-links a:last-child {
      border: 0;
    }

    .nav-actions {
      display: none;
    }

    .is-open .nav-actions {
      position: absolute;
      top: 280px;
      right: 0;
      left: 0;
      z-index: 2;
      display: flex;
      justify-content: space-between;
      padding: 14px 18px;
      background: var(--paper);
      border: 1px solid var(--line);
      border-top: 0;
    }

    .hero {
      display: block;
      min-height: auto;
      padding-top: 75px;
      padding-bottom: 70px;
    }

    h1 {
      max-width: 440px;
      font-size: clamp(49px, 15vw, 70px);
    }

    .hero-lead {
      font-size: 14px;
    }

    .hero-footnote {
      margin-top: 45px;
    }

    .hero-system {
      width: 100%;
      height: 375px;
      margin: 70px 0 0;
      transform: scale(0.8);
      transform-origin: top center;
    }

    .signal-inner {
      display: block;
      padding: 17px 0;
    }

    .signal-label {
      display: block;
      margin-bottom: 13px;
    }

    .signal-item {
      margin-right: 18px;
      margin-left: 0;
    }

    .signal-item:nth-of-type(n + 3) {
      display: none;
    }

    .section {
      padding-top: 90px;
      padding-bottom: 90px;
    }

    .section-intro,
    .preview-heading,
    .architecture-section,
    .workflow-layout {
      display: block;
    }

    .section-intro h2 {
      margin-bottom: 24px;
    }

    .section-intro > p:last-child {
      max-width: 340px;
    }

    .section-index {
      margin-bottom: 19px;
    }

    .capability-grid {
      grid-template-columns: 1fr;
      gap: 10px;
    }

    .capability-card {
      min-height: 295px;
    }

    .card-icon {
      margin-top: 35px;
      margin-bottom: 25px;
    }

    .workflow-layout {
      padding-top: 90px;
      padding-bottom: 90px;
    }

    .workflow-heading {
      margin-bottom: 55px;
    }

    .workflow-step {
      grid-template-columns: 38px 1fr;
      gap: 12px;
      min-height: 135px;
    }

    .step-mark {
      grid-column: 2;
      margin-top: -42px;
    }

    .architecture-intro {
      margin-bottom: 45px;
    }

    .architecture-map {
      padding: 16px 10px;
    }

    .map-layers {
      padding: 28px 2px;
    }

    .map-layer {
      grid-template-columns: 29px 1fr 30px;
      padding: 0 9px;
    }

    .map-layer small {
      display: none;
    }

    .preview-section {
      padding: 90px 0 95px;
    }

    .preview-heading {
      margin-bottom: 38px;
    }

    .preview-heading h2 {
      margin-bottom: 20px;
      font-size: 38px;
    }

    .preview-heading .text-link {
      display: inline-block;
      margin-top: 25px;
    }

    .dashboard-frame {
      box-shadow: 8px 8px 0 rgb(243 111 63 / 72%);
    }

    .frame-body {
      display: block;
    }

    .mock-sidebar {
      display: none;
    }

    .mock-content {
      padding: 20px 14px;
    }

    .mock-grid {
      grid-template-columns: 1fr 1fr;
    }

    .metric-card {
      min-height: 143px;
    }

    .metric-primary {
      grid-column: span 2;
    }

    .compact {
      grid-column: span 1;
    }

    .run-card,
    .activity-card {
      grid-column: span 2;
    }

    .activity-chart {
      gap: 3px;
      height: 105px;
    }

    .closing-section {
      padding-top: 100px;
      padding-bottom: 95px;
    }

    .closing-mark {
      top: 55px;
      right: 0;
      font-size: 90px;
    }

    .closing-section h2 {
      font-size: 43px;
    }

    .docs-footer {
      flex-wrap: wrap;
      gap: 15px;
      padding: 22px 0;
    }

    .docs-footer > span:not(.brand-word) {
      font-size: 8px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .reveal-up,
    .reveal-on-scroll,
    .reveal-on-scroll.is-visible,
    .pulse-dot,
    .system-node,
    .system-orbit,
    .mini-chart i,
    .activity-chart span,
    .ring-chart {
      opacity: 1;
      transform: none;
      translate: none;
      animation: none;
    }

    .button,
    .nav-cta,
    .capability-card {
      transition: none;
    }
  }
</style>

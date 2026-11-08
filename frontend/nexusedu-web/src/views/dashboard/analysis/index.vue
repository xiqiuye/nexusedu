<template>
  <div class="education-analysis">
    <header class="analysis-heading">
      <div>
        <div class="eyebrow">NEXUSEDU / ANALYTICS</div>
        <h1>教学与 AI 分析</h1>
        <p>关注学业变化，了解智能助手的使用情况。</p>
      </div>
      <div class="data-note">
        <ElTag type="warning" effect="plain" round>模拟数据</ElTag>
        <span>演示截止日期 · 2026.11.08</span>
      </div>
    </header>

    <section class="section-heading" aria-labelledby="academic-heading">
      <div><h2 id="academic-heading">学业概览</h2><span>2026–2027 学年 · 第一学期</span></div>
      <ElSelect v-model="selectedGrade" aria-label="选择统计年级" class="grade-select">
        <ElOption label="全部年级" value="all" />
        <ElOption
          v-for="grade in grades"
          :key="grade.name"
          :label="grade.name"
          :value="grade.name"
        />
      </ElSelect>
    </section>
    <div class="metrics-grid">
      <article v-for="metric in academicMetrics" :key="metric.label" class="art-card metric-card">
        <div class="metric-label"
          ><span>{{ metric.label }}</span
          ><ArtSvgIcon :icon="metric.icon"
        /></div>
        <div class="metric-value"
          >{{ metric.value }}<small>{{ metric.unit }}</small></div
        >
        <p>{{ metric.note }}</p>
      </article>
    </div>
    <div class="academic-grid">
      <article class="art-card chart-card">
        <div class="chart-heading"
          ><div><h3>学生成绩趋势</h3><p>各次考试学科均分 · 满分 100 分</p></div
          ><ElTag effect="plain">{{ gradeLabel }}</ElTag></div
        >
        <ArtLineChart
          height="300px"
          :data="scoreTrend"
          :x-axis-data="examLabels"
          :colors="chartColors"
          :show-legend="true"
          :show-axis-line="false"
          :smooth="false"
          symbol="circle"
        />
      </article>
      <article class="art-card chart-card">
        <div class="chart-heading"
          ><div
            ><h3>学生成绩分布</h3><p>期中综合均分 · {{ format(studentCount) }} 名学生</p></div
          ><span class="unit-label">单位：人</span></div
        >
        <ArtBarChart
          height="270px"
          :data="[{ name: '学生人数', data: distribution }]"
          :x-axis-data="scoreBands"
          :colors="['#4b82db']"
          :show-axis-line="false"
          bar-width="38%"
        />
        <div class="chart-footnote"><span>及格 ≥ 60 分</span><span>优秀 ≥ 90 分</span></div>
      </article>
    </div>
    <article class="art-card subject-card">
      <div class="chart-heading"
        ><div><h3>学科成绩统计</h3><p>期中考试 · 与 10 月月考对比，分数按百分制展示</p></div
        ><span class="unit-label">{{ gradeLabel }}</span></div
      >
      <ElTable
        :data="subjectStats"
        style="width: 100%"
        :header-cell-style="{ background: 'transparent' }"
      >
        <ElTableColumn prop="name" label="学科" min-width="90" />
        <ElTableColumn label="平均分" min-width="110"
          ><template #default="{ row }"
            ><strong>{{ row.average.toFixed(1) }}</strong
            ><span class="table-unit"> / 100</span></template
          ></ElTableColumn
        >
        <ElTableColumn label="较上次考试" min-width="120"
          ><template #default="{ row }"
            ><span :class="row.average >= row.previous ? 'positive' : 'negative'"
              >{{ row.average >= row.previous ? '+' : ''
              }}{{ (row.average - row.previous).toFixed(1) }} 分</span
            ></template
          ></ElTableColumn
        >
        <ElTableColumn label="及格率" min-width="160"
          ><template #default="{ row }"
            ><div class="rate-cell"
              ><ElProgress
                :percentage="row.pass"
                :show-text="false"
                :stroke-width="6"
                color="#269a83"
              /><span>{{ row.pass.toFixed(1) }}%</span></div
            ></template
          ></ElTableColumn
        >
        <ElTableColumn label="优秀率" min-width="110"
          ><template #default="{ row }">{{ row.excellent.toFixed(1) }}%</template></ElTableColumn
        >
      </ElTable>
      <p class="table-note"
        >综合均分为各学科等权平均；全部年级按参评人数加权。学科及格率、优秀率为对应学科口径。</p
      >
    </article>

    <section class="section-heading ai-heading" aria-labelledby="ai-heading">
      <div
        ><h2 id="ai-heading">AI 助手洞察</h2
        ><span>全校使用情况 · {{ aiDays[0].date }}–{{ aiDays[aiDays.length - 1].date }}</span></div
      >
      <ElRadioGroup v-model="aiPeriod" size="small" aria-label="AI 统计时间范围">
        <ElRadioButton :value="7">近 7 天</ElRadioButton
        ><ElRadioButton :value="14">近 14 天</ElRadioButton>
      </ElRadioGroup>
    </section>
    <div class="metrics-grid ai-metrics">
      <article v-for="metric in aiMetrics" :key="metric.label" class="art-card metric-card">
        <div class="metric-label"
          ><span>{{ metric.label }}</span
          ><ArtSvgIcon :icon="metric.icon"
        /></div>
        <div class="metric-value"
          >{{ metric.value }}<small>{{ metric.unit }}</small></div
        >
        <p>{{ metric.note }}</p>
      </article>
    </div>
    <div class="ai-grid">
      <article class="art-card chart-card ai-trend-card">
        <div class="chart-heading"
          ><div><h3>对话请求与会话趋势</h3><p>请求按消息计数，会话按新建次数计数</p></div
          ><span class="unit-label">单位：次</span></div
        >
        <ArtLineChart
          height="280px"
          :data="requestTrend"
          :x-axis-data="aiLabels"
          :colors="chartColors"
          :show-legend="true"
          :show-axis-line="false"
          :show-area-color="true"
        />
      </article>
      <article class="art-card chart-card scenario-card">
        <div class="chart-heading"
          ><div><h3>AI 使用场景</h3><p>按请求的主要意图分类</p></div></div
        >
        <ArtRingChart
          height="210px"
          :data="scenarios"
          :colors="scenarioColors"
          :radius="['62%', '79%']"
          :center-text="`${format(aiTotals.requests)}\n请求总数`"
        />
        <div class="scenario-legend"
          ><div v-for="(item, index) in scenarios" :key="item.name"
            ><span class="legend-dot" :style="{ background: scenarioColors[index] }"></span
            ><span>{{ item.name }}</span
            ><strong>{{ format(item.value) }}</strong
            ><small>{{ ((item.value / aiTotals.requests) * 100).toFixed(1) }}%</small></div
          ></div
        >
      </article>
      <article class="art-card chart-card">
        <div class="chart-heading"
          ><div><h3>请求执行统计</h3><p>成功与失败请求 · 单位：次</p></div></div
        >
        <ArtBarChart
          height="260px"
          :data="executionStats"
          :x-axis-data="aiLabels"
          :colors="['#269a83', '#e5a24b']"
          :stack="true"
          :show-legend="true"
          :show-axis-line="false"
          bar-width="45%"
        />
        <div class="chart-footnote"
          ><span>成功率 {{ successRate }}%</span
          ><span>失败 {{ format(aiTotals.failed) }} 次</span></div
        >
      </article>
      <article class="art-card chart-card">
        <div class="chart-heading"
          ><div><h3>首 Token 响应耗时</h3><p>每日平均首 Token 等待时间 · 单位：秒</p></div></div
        >
        <ArtLineChart
          height="260px"
          :data="[{ name: '首 Token 耗时（秒）', data: aiDays.map((day) => day.firstToken) }]"
          :x-axis-data="aiLabels"
          :colors="['#269a83']"
          :show-axis-line="false"
          symbol="circle"
        />
        <div class="chart-footnote"
          ><span>按每日请求数加权</span><span>期间均值 {{ averageFirstToken }} 秒</span></div
        >
      </article>
      <article class="art-card chart-card token-card">
        <div class="chart-heading"
          ><div><h3>Token 用量趋势</h3><p>模型输入与输出 Token 合计 · 单位：千 Token</p></div
          ><strong class="token-total"
            >{{ (aiTotals.tokens / 1_000_000).toFixed(2) }}<small> M Token</small></strong
          ></div
        >
        <ArtBarChart
          height="260px"
          :data="[{ name: '千 Token', data: aiDays.map((day) => day.tokens / 1000) }]"
          :x-axis-data="aiLabels"
          :colors="['#4b82db']"
          :show-axis-line="false"
          bar-width="40%"
        />
        <div class="chart-footnote"
          ><span>期间累计 {{ format(aiTotals.tokens) }} Token</span
          ><span>仅展示用量，不估算费用</span></div
        >
      </article>
    </div>
    <footer class="analysis-footer"
      >以上为固定模拟数据，用于演示教学分析与 AI 统计，不代表实际学生成绩或服务运行情况。</footer
    >
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { grades, examLabels, scoreBands, aiDaily, requestCount } from './mock-data'
  import type { LineDataItem } from '@/types/component/chart'

  defineOptions({ name: 'Analysis' })

  const selectedGrade = ref('all')
  const aiPeriod = ref(7)
  const chartColors = ['#4b82db', '#269a83', '#e5a24b']
  const scenarioColors = ['#4b82db', '#269a83', '#e5a24b', '#92a3bd']
  const format = (value: number) => value.toLocaleString('zh-CN')
  const visibleGrades = computed(() =>
    grades.filter((grade) => selectedGrade.value === 'all' || grade.name === selectedGrade.value)
  )
  const gradeLabel = computed(() =>
    selectedGrade.value === 'all' ? '全部年级' : selectedGrade.value
  )
  const studentCount = computed(() =>
    visibleGrades.value.reduce((sum, grade) => sum + grade.students, 0)
  )
  const distribution = computed(() =>
    scoreBands.map((_, index) =>
      visibleGrades.value.reduce((sum, grade) => sum + grade.distribution[index], 0)
    )
  )
  const weighted = (getValue: (grade: (typeof grades)[number]) => number) =>
    visibleGrades.value.reduce((sum, grade) => sum + getValue(grade) * grade.students, 0) /
    studentCount.value
  const scoreTrend = computed<LineDataItem[]>(() =>
    visibleGrades.value.map((grade) => ({ name: grade.name, data: grade.trend }))
  )
  const subjectStats = computed(() =>
    grades[0].subjects.map((subject, index) => ({
      name: subject.name,
      average: weighted((grade) => grade.subjects[index].average),
      previous: weighted((grade) => grade.subjects[index].previous),
      pass: weighted((grade) => grade.subjects[index].pass),
      excellent: weighted((grade) => grade.subjects[index].excellent)
    }))
  )
  const academicMetrics = computed(() => [
    {
      label: '参评学生',
      value: format(studentCount.value),
      unit: '人',
      note: `${gradeLabel.value} · 期中考试`,
      icon: 'ri:graduation-cap-line'
    },
    {
      label: '期中综合均分',
      value: (
        subjectStats.value.reduce((sum, subject) => sum + subject.average, 0) /
        subjectStats.value.length
      ).toFixed(1),
      unit: '分',
      note: '五门学科等权 · 百分制',
      icon: 'ri:line-chart-line'
    },
    {
      label: '综合及格率',
      value: (((studentCount.value - distribution.value[0]) / studentCount.value) * 100).toFixed(1),
      unit: '%',
      note: '综合均分达到 60 分',
      icon: 'ri:checkbox-circle-line'
    },
    {
      label: '综合优秀率',
      value: ((distribution.value[4] / studentCount.value) * 100).toFixed(1),
      unit: '%',
      note: '综合均分达到 90 分',
      icon: 'ri:medal-line'
    }
  ])
  const aiDays = computed(() => aiDaily.slice(-aiPeriod.value))
  const aiLabels = computed(() => aiDays.value.map((day) => day.date))
  const aiTotals = computed(() =>
    aiDays.value.reduce(
      (sum, day) => ({
        requests: sum.requests + requestCount(day),
        failed: sum.failed + day.failed,
        sessions: sum.sessions + day.sessions,
        users: sum.users + day.users,
        tokens: sum.tokens + day.tokens,
        wait: sum.wait + day.firstToken * requestCount(day)
      }),
      { requests: 0, failed: 0, sessions: 0, users: 0, tokens: 0, wait: 0 }
    )
  )
  const successRate = computed(() =>
    (((aiTotals.value.requests - aiTotals.value.failed) / aiTotals.value.requests) * 100).toFixed(1)
  )
  const averageFirstToken = computed(() =>
    (aiTotals.value.wait / aiTotals.value.requests).toFixed(2)
  )
  const aiMetrics = computed(() => [
    {
      label: '对话请求数',
      value: format(aiTotals.value.requests),
      unit: '次',
      note: `近 ${aiPeriod.value} 天 · 全校消息请求`,
      icon: 'ri:chat-3-line'
    },
    {
      label: '新建会话',
      value: format(aiTotals.value.sessions),
      unit: '次',
      note: `日均活跃用户 ${Math.round(aiTotals.value.users / aiDays.value.length)} 人`,
      icon: 'ri:discuss-line'
    },
    {
      label: '请求成功率',
      value: successRate.value,
      unit: '%',
      note: `成功 ${format(aiTotals.value.requests - aiTotals.value.failed)} / 总计 ${format(aiTotals.value.requests)}`,
      icon: 'ri:shield-check-line'
    },
    {
      label: '平均首 Token 耗时',
      value: averageFirstToken.value,
      unit: '秒',
      note: '从提交到首个 Token 返回',
      icon: 'ri:timer-line'
    }
  ])
  const requestTrend = computed<LineDataItem[]>(() => [
    { name: '对话请求', data: aiDays.value.map(requestCount) },
    { name: '新建会话', data: aiDays.value.map((day) => day.sessions) }
  ])
  const scenarios = computed(() => [
    { name: '学生信息查询', value: aiDays.value.reduce((sum, day) => sum + day.student, 0) },
    { name: '知识库问答', value: aiDays.value.reduce((sum, day) => sum + day.knowledge, 0) },
    { name: '学习辅导', value: aiDays.value.reduce((sum, day) => sum + day.learning, 0) },
    { name: '通用对话', value: aiDays.value.reduce((sum, day) => sum + day.other, 0) }
  ])
  const executionStats = computed(() => [
    { name: '成功', data: aiDays.value.map((day) => requestCount(day) - day.failed) },
    { name: '失败', data: aiDays.value.map((day) => day.failed) }
  ])
</script>

<style scoped lang="scss">
  .education-analysis {
    padding-bottom: 12px;
    color: var(--el-text-color-primary);
  }

  .analysis-heading,
  .section-heading,
  .section-heading > div,
  .chart-heading,
  .metric-label,
  .chart-footnote {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
  }

  .analysis-heading {
    padding: 8px 0 26px;
  }

  .eyebrow {
    margin-bottom: 8px;
    font-size: 11px;
    font-weight: 600;
    color: var(--el-color-primary);
    letter-spacing: 2px;
  }

  h1 {
    margin: 0;
    font-size: 26px;
    font-weight: 650;
    letter-spacing: -0.5px;
  }

  .analysis-heading p {
    margin: 8px 0 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .data-note {
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: flex-end;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .section-heading {
    margin-bottom: 16px;
  }

  h2 {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
  }

  .section-heading span,
  .unit-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .grade-select {
    width: 148px;
  }

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 18px;
    margin-bottom: 18px;
  }

  .metric-card {
    padding: 20px 22px;
  }

  .metric-label {
    font-size: 13px;
    color: var(--el-text-color-regular);
  }

  .metric-label .art-svg-icon {
    font-size: 21px;
    color: #4b82db;
  }

  .metric-value {
    margin: 16px 0 8px;
    font-size: 30px;
    font-weight: 650;
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
    letter-spacing: -0.6px;
  }

  .metric-value small {
    margin-left: 6px;
    font-size: 13px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }

  .metric-card p {
    margin: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .academic-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
    gap: 18px;
    margin-bottom: 18px;
  }

  .chart-card,
  .subject-card {
    min-width: 0;
    padding: 22px;
  }

  .chart-heading {
    align-items: flex-start;
    margin-bottom: 16px;
  }

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
  }

  .chart-heading p {
    margin: 7px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .unit-label {
    white-space: nowrap;
  }

  .chart-footnote {
    flex-wrap: wrap;
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .subject-card {
    margin-bottom: 28px;
  }

  .rate-cell {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .rate-cell .el-progress {
    flex: 1;
    max-width: 180px;
  }

  .rate-cell span {
    min-width: 45px;
    font-variant-numeric: tabular-nums;
  }

  .table-unit,
  .table-note {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .table-note {
    margin: 14px 0 0;
    line-height: 1.7;
  }

  .positive {
    color: var(--el-color-success);
  }

  .negative {
    color: var(--el-color-danger);
  }

  .ai-heading {
    padding-top: 4px;
  }

  .ai-metrics .metric-label .art-svg-icon {
    color: #269a83;
  }

  .ai-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
  }

  .ai-trend-card {
    grid-column: span 2;
  }

  .scenario-card {
    grid-row: span 2;
  }

  .scenario-legend {
    display: grid;
    gap: 20px;
    margin: 20px 0 0;
  }

  .scenario-legend > div {
    display: grid;
    grid-template-columns: 8px minmax(0, 1fr) auto 48px;
    gap: 10px;
    align-items: center;
    font-size: 13px;
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .scenario-legend small {
    font-variant-numeric: tabular-nums;
    color: var(--el-text-color-secondary);
    text-align: right;
  }

  .token-card {
    grid-column: 1 / -1;
  }

  .token-total {
    font-size: 24px;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .token-total small {
    font-size: 12px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }

  .analysis-footer {
    padding: 20px 0 0;
    font-size: 12px;
    line-height: 1.7;
    color: var(--el-text-color-secondary);
  }

  @media (width <= 1199px) {
    .metrics-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .ai-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .ai-trend-card {
      grid-column: 1 / -1;
    }

    .scenario-card {
      grid-row: auto;
    }
  }

  @media (width <= 767px) {
    .analysis-heading {
      flex-direction: column;
      gap: 14px;
      align-items: flex-start;
      padding-bottom: 22px;
    }

    .data-note {
      flex-flow: row wrap;
      align-items: center;
    }

    .section-heading > div {
      flex-direction: column;
      gap: 5px;
      align-items: flex-start;
    }

    .academic-grid,
    .ai-grid {
      grid-template-columns: minmax(0, 1fr);
    }

    .ai-trend-card,
    .token-card {
      grid-column: auto;
    }

    .metrics-grid {
      gap: 12px;
    }

    .metric-card {
      padding: 16px;
    }

    .metric-value {
      font-size: 26px;
    }

    .chart-card,
    .subject-card {
      padding: 16px;
    }

    .chart-heading {
      flex-wrap: wrap;
      gap: 10px;
    }

    .ai-heading {
      flex-wrap: wrap;
      align-items: flex-start;
    }

    .scenario-legend {
      margin-top: 4px;
    }
  }
</style>

<template>
  <div class="evaluation-page">
    <ElCard>
      <div class="toolbar">
        <ElInput v-model="dataset" placeholder="数据集，例如 smoke" @keyup.enter="resetPage" />
        <ElButton :loading="loading" @click="resetPage">刷新</ElButton>
        <ElButton v-if="hasAuth('ai:evaluation:write')" type="primary" @click="openCase()"
          >新增样本版本</ElButton
        >
        <ElButton v-if="hasAuth('ai:evaluation:run')" type="success" @click="runVisible = true"
          >执行评测</ElButton
        >
        <ElButton v-if="hasAuth('ai:evaluation:run')" @click="openSchedule()">定期评测</ElButton>
      </div>
      <p
        >评测集保存标准规划、工具、来源和拒答期望。每个批次固定最新样本快照，使用真实模型和只读查询工具；批次及版本记录保留，可重复运行比较指标。</p
      >
      <ElTabs v-model="tab" @tab-change="resetPage"
        ><ElTabPane label="评测集" name="cases" /><ElTabPane
          label="批次与指标趋势"
          name="batches" /><ElTabPane label="定期计划" name="schedules"
      /></ElTabs>
      <ElTable :data="rows" v-loading="loading">
        <ElTableColumn prop="dataset" label="数据集" width="130" />
        <template v-if="tab === 'cases'">
          <ElTableColumn prop="caseKey" label="样本标识" min-width="160" />
          <ElTableColumn prop="version" label="版本" width="75" />
          <ElTableColumn prop="question" label="问题" min-width="250" show-overflow-tooltip />
          <ElTableColumn label="启用" width="85"
            ><template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template></ElTableColumn
          >
          <ElTableColumn label="操作" width="220"
            ><template #default="{ row }"
              ><ElButton link @click="showDetails(row)">查看标准</ElButton
              ><ElButton v-if="hasAuth('ai:evaluation:write')" link @click="openCase(row)"
                >新版本</ElButton
              ><ElButton
                v-if="hasAuth('ai:evaluation:write')"
                link
                type="warning"
                @click="toggle(row)"
                >{{ row.enabled ? '停用' : '启用' }}</ElButton
              ></template
            ></ElTableColumn
          >
        </template>
        <template v-else-if="tab === 'batches'">
          <ElTableColumn prop="id" label="批次 ID" min-width="190" />
          <ElTableColumn prop="modelAlias" label="模型别名" width="110" />
          <ElTableColumn prop="status" label="状态" width="110" />
          <ElTableColumn prop="createTime" label="开始时间" min-width="180" />
          <ElTableColumn
            v-for="metric in trendMetrics"
            :key="metric.key"
            :label="metric.label"
            min-width="105"
            ><template #default="{ row }">{{
              percent(row.summary?.[metric.key])
            }}</template></ElTableColumn
          >
          <ElTableColumn label="成本" min-width="150"
            ><template #default="{ row }"
              >{{ costs(row.summary?.costs)
              }}<small v-if="Number(row.summary?.unknownCostCalls) > 0"
                >（含未知费用）</small
              ></template
            ></ElTableColumn
          >
          <ElTableColumn label="结果" width="80" fixed="right"
            ><template #default="{ row }"
              ><ElButton link @click="openBatch(row)">详情</ElButton></template
            ></ElTableColumn
          >
        </template>
        <template v-else>
          <ElTableColumn prop="intervalHours" label="间隔（小时）" width="130" />
          <ElTableColumn label="启用" width="90"
            ><template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template></ElTableColumn
          >
          <ElTableColumn prop="nextRunAt" label="下次执行" min-width="180" />
          <ElTableColumn prop="lastError" label="暂停原因" min-width="250" />
          <ElTableColumn label="操作" width="80"
            ><template #default="{ row }"
              ><ElButton v-if="hasAuth('ai:evaluation:run')" link @click="openSchedule(row)"
                >修改</ElButton
              ></template
            ></ElTableColumn
          >
        </template>
      </ElTable>
      <ElPagination
        v-model:current-page="page"
        :total="total"
        :page-size="20"
        layout="total, prev, pager, next"
        @current-change="reload"
      />
    </ElCard>
    <ElDialog v-model="caseVisible" title="新增评测样本版本" width="780px">
      <ElForm label-width="120px">
        <ElFormItem label="数据集"><ElInput v-model="sample.dataset" /></ElFormItem>
        <ElFormItem label="样本标识"><ElInput v-model="sample.caseKey" /></ElFormItem>
        <ElFormItem label="版本"
          ><ElInputNumber v-model="sample.version" :min="1" :max="1000000"
        /></ElFormItem>
        <ElFormItem label="问题"
          ><ElInput v-model="sample.question" type="textarea" :rows="2" maxlength="4000"
        /></ElFormItem>
        <ElFormItem label="标准规划 JSON"
          ><ElInput v-model="sample.expectedPlan" type="textarea" :rows="5"
        /></ElFormItem>
        <ElFormItem label="标准工具 JSON"
          ><ElInput v-model="sample.expectedTools" placeholder='["query_knowledge"]'
        /></ElFormItem>
        <ElFormItem label="标准来源 JSON"
          ><ElInput v-model="sample.expectedSources" placeholder='["documentNo#chunkNo"]'
        /></ElFormItem>
        <ElFormItem label="期望拒答"><ElSwitch v-model="sample.expectedRefusal" /></ElFormItem>
      </ElForm>
      <p
        >标准规划按顺序匹配 domain、toolName 和填写的 query
        字段。工具按集合匹配。标准来源为空时不纳入 RAG 命中率；引用按回答中的 [documentNo#chunkNo]
        与检索、标准来源核验。明确拒答使用【拒答】前缀。</p
      >
      <template #footer
        ><ElButton @click="caseVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitCase"
          >保存版本</ElButton
        ></template
      >
    </ElDialog>
    <ElDialog v-model="runVisible" title="执行离线评测" width="550px">
      <ElForm label-width="130px">
        <ElFormItem label="数据集"><ElInput v-model="dataset" /></ElFormItem>
        <ElFormItem label="模型别名"><ElInput v-model="runConfig.modelAlias" /></ElFormItem>
        <ElFormItem label="模型版本 ID"
          ><ElInput v-model="runConfig.modelVersionId" placeholder="可选：留空使用当前发布"
        /></ElFormItem>
        <ElFormItem label="规划提示词版本 ID"
          ><ElInput v-model="runConfig.promptVersionId" placeholder="可选：支持草稿 / 待审批版本"
        /></ElFormItem>
      </ElForm>
      <p
        >每批 1–200
        个启用的最新样本，会产生真实调用费用；评测同样受权限、限流和预算约束。指定版本仅用于该批次，不改变线上发布。</p
      >
      <template #footer
        ><ElButton @click="runVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitRun">运行</ElButton></template
      >
    </ElDialog>
    <ElDialog v-model="scheduleVisible" title="定期评测" width="520px">
      <ElForm label-width="110px"
        ><ElFormItem label="数据集"><ElInput v-model="schedule.dataset" /></ElFormItem
        ><ElFormItem label="间隔（小时）"
          ><ElInputNumber v-model="schedule.intervalHours" :min="1" :max="8760" /></ElFormItem
        ><ElFormItem label="启用"><ElSwitch v-model="schedule.enabled" /></ElFormItem
      ></ElForm>
      <p
        >使用当前发布配置，保存后首次立即调度。执行时重新校验创建者的认证会话与权限；会话失效或启动失败会暂停计划，重新登录后保存即可恢复。</p
      >
      <template #footer
        ><ElButton @click="scheduleVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitSchedule"
          >保存</ElButton
        ></template
      >
    </ElDialog>
    <ElDrawer v-model="batchVisible" title="批次指标与逐条结果" size="85%">
      <ElButton :loading="resultLoading" @click="loadBatch">刷新结果</ElButton>
      <p
        >完成 {{ summary?.completed ?? 0 }} / {{ summary?.total ?? 0 }}，执行失败
        {{ summary?.failed ?? 0 }}，已知成本 {{ costs(summary?.costs) }}，未知费用调用
        {{ summary?.unknownCostCalls ?? 0 }}</p
      >
      <div class="metrics"
        ><ElCard v-for="metric in trendMetrics" :key="metric.key"
          ><small>{{ metric.label }}</small
          ><h3>{{ percent(summary?.[metric.key]) }}</h3></ElCard
        ></div
      >
      <p
        >无适用样本、无引用或未产出回答时显示 “—”。RAG
        命中以标准来源实际被检索到为准；引用正确率验证来源标识，不等同于语义事实正确性。拒答率同时展示期望匹配率。</p
      >
      <p>版本快照：{{ JSON.stringify(summary?.releases ?? []) }}</p>
      <ElTable :data="results" v-loading="resultLoading">
        <ElTableColumn label="样本" min-width="150"
          ><template #default="{ row }"
            >{{ row.caseSnapshot?.caseKey }} v{{ row.caseSnapshot?.version }}</template
          ></ElTableColumn
        >
        <ElTableColumn prop="status" label="执行状态" width="110" />
        <ElTableColumn prop="planCorrect" label="规划正确" width="100" />
        <ElTableColumn prop="toolCorrect" label="工具正确" width="100" />
        <ElTableColumn prop="ragHit" label="RAG 命中" width="100" />
        <ElTableColumn label="正确引用 / 引用数" width="165"
          ><template #default="{ row }"
            >{{ row.correctCitedCount ?? '—' }} / {{ row.citedCount ?? '—' }}</template
          ></ElTableColumn
        >
        <ElTableColumn prop="refused" label="拒答" width="80" />
        <ElTableColumn prop="refusalCorrect" label="拒答匹配" width="100" />
        <ElTableColumn prop="errorMessage" label="错误" min-width="160" show-overflow-tooltip />
        <ElTableColumn label="回答与快照" width="120"
          ><template #default="{ row }"
            ><ElButton link @click="showDetails(row)">查看</ElButton></template
          ></ElTableColumn
        >
      </ElTable>
      <ElPagination
        v-model:current-page="resultPage"
        :total="resultTotal"
        :page-size="20"
        layout="total, prev, pager, next"
        @current-change="loadBatch"
      />
    </ElDrawer>
    <ElDialog v-model="detailVisible" title="样本 / 执行快照" width="800px">
      <pre>{{ JSON.stringify(detail, null, 2) }}</pre>
    </ElDialog>
  </div>
</template>
<script setup lang="ts">
  import { ElMessage } from 'element-plus'
  import { useAuth } from '@/hooks/core/useAuth'
  import {
    operationList,
    addEvaluationCase,
    enableEvaluationCase,
    runEvaluation,
    evaluationSummary,
    saveEvaluationSchedule
  } from '@/api/ai-operations'
  import type { ManagementRow } from '@/api/management'
  defineOptions({ name: 'AiEvaluation' })
  const { hasAuth } = useAuth()
  const dataset = ref('smoke'),
    tab = ref('cases'),
    page = ref(1),
    total = ref(0),
    loading = ref(false),
    rows = ref<ManagementRow[]>([]),
    saving = ref(false)
  const caseVisible = ref(false),
    runVisible = ref(false),
    scheduleVisible = ref(false),
    batchVisible = ref(false),
    detailVisible = ref(false),
    detail = ref<ManagementRow>()
  const sample = reactive({
    dataset: '',
    caseKey: '',
    version: 1,
    question: '',
    expectedPlan: '[{"domain":"chat","toolName":null}]',
    expectedTools: '[]',
    expectedSources: '[]',
    expectedRefusal: false
  })
  const runConfig = reactive({ modelAlias: 'balanced', modelVersionId: '', promptVersionId: '' })
  function showDetails(row: ManagementRow) {
    detail.value = row
    detailVisible.value = true
  }
  const schedule = reactive({ dataset: '', intervalHours: 24, enabled: true })
  const batchId = ref(''),
    summary = ref<Record<string, any>>(),
    results = ref<ManagementRow[]>([]),
    resultPage = ref(1),
    resultTotal = ref(0),
    resultLoading = ref(false)
  const trendMetrics = [
    { key: 'planningAccuracy', label: '规划准确率' },
    { key: 'toolAccuracy', label: '工具选择准确率' },
    { key: 'ragHitRate', label: 'RAG 命中率' },
    { key: 'citationCorrectness', label: '引用正确率' },
    { key: 'refusalRate', label: '拒答率' },
    { key: 'refusalAccuracy', label: '拒答匹配率' }
  ]
  const percent = (value: unknown) => (value == null ? '—' : `${(Number(value) * 100).toFixed(1)}%`)
  const costs = (items?: { currency: string; amount: string }[]) =>
    items?.length ? items.map((item) => `${item.currency} ${item.amount}`).join(' / ') : '—'
  let generation = 0
  async function reload() {
    if (!hasAuth('ai:evaluation:read')) return
    const requestGeneration = ++generation
    const resource = tab.value
    loading.value = true
    try {
      const result = await operationList('evaluation', resource, {
        dataset: dataset.value,
        current: page.value,
        size: 20
      })
      if (requestGeneration !== generation) return
      if (resource === 'batches')
        await Promise.all(
          result.records.map(async (row) => {
            row.summary = await evaluationSummary(row.id)
          })
        )
      if (requestGeneration !== generation) return
      rows.value = result.records
      total.value = result.total
    } finally {
      if (requestGeneration === generation) loading.value = false
    }
  }
  function resetPage() {
    page.value = 1
    reload()
  }
  function openCase(row?: ManagementRow) {
    Object.assign(sample, {
      dataset: row?.dataset ?? dataset.value,
      caseKey: row?.caseKey ?? '',
      version: row ? Number(row.version) + 1 : 1,
      question: row?.question ?? '',
      expectedPlan: row
        ? JSON.stringify(row.expectedPlan, null, 2)
        : '[{"domain":"chat","toolName":null}]',
      expectedTools: JSON.stringify(row?.expectedTools ?? []),
      expectedSources: JSON.stringify(row?.expectedSources ?? []),
      expectedRefusal: !!row?.expectedRefusal
    })
    caseVisible.value = true
  }
  async function submitCase() {
    let expectedPlan: unknown, expectedTools: unknown, expectedSources: unknown
    try {
      expectedPlan = JSON.parse(sample.expectedPlan)
      expectedTools = JSON.parse(sample.expectedTools)
      expectedSources = JSON.parse(sample.expectedSources)
      if (![expectedPlan, expectedTools, expectedSources].every(Array.isArray)) throw new Error()
    } catch {
      ElMessage.warning('标准规划、工具和来源须为有效 JSON 数组')
      return
    }
    saving.value = true
    try {
      await addEvaluationCase({ ...sample, expectedPlan, expectedTools, expectedSources })
      caseVisible.value = false
      await reload()
    } finally {
      saving.value = false
    }
  }
  async function toggle(row: ManagementRow) {
    await enableEvaluationCase(row.id, !row.enabled)
    await reload()
  }
  async function submitRun() {
    saving.value = true
    try {
      const result = await runEvaluation({
        ...runConfig,
        dataset: dataset.value,
        modelVersionId: runConfig.modelVersionId || null,
        promptVersionId: runConfig.promptVersionId || null
      })
      runVisible.value = false
      tab.value = 'batches'
      page.value = 1
      await reload()
      await openBatch(result)
    } finally {
      saving.value = false
    }
  }
  function openSchedule(row?: ManagementRow) {
    Object.assign(schedule, {
      dataset: row?.dataset ?? dataset.value,
      intervalHours: row?.intervalHours ?? 24,
      enabled: row ? !!row.enabled : true
    })
    scheduleVisible.value = true
  }
  async function submitSchedule() {
    saving.value = true
    try {
      await saveEvaluationSchedule({ ...schedule })
      scheduleVisible.value = false
      tab.value = 'schedules'
      page.value = 1
      await reload()
    } finally {
      saving.value = false
    }
  }
  async function openBatch(row: ManagementRow) {
    batchId.value = row.id
    resultPage.value = 1
    batchVisible.value = true
    await loadBatch()
  }
  async function loadBatch() {
    resultLoading.value = true
    try {
      const [overview, result] = await Promise.all([
        evaluationSummary(batchId.value),
        operationList('evaluation', 'results', {
          batchId: batchId.value,
          current: resultPage.value,
          size: 20
        })
      ])
      summary.value = overview
      results.value = result.records
      resultTotal.value = result.total
    } finally {
      resultLoading.value = false
    }
  }
  onMounted(reload)
</script>
<style scoped>
  .evaluation-page {
    display: grid;
    gap: 16px;
    padding: 16px;
  }
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  .toolbar .el-input {
    width: 250px;
  }
  .metrics {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 10px;
  }
  p {
    color: var(--el-text-color-secondary);
    line-height: 1.6;
  }
  pre {
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
  .el-pagination {
    margin-top: 16px;
  }
</style>

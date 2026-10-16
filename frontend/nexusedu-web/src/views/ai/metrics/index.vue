<template>
  <div class="metrics-page">
    <ElCard>
      <div class="toolbar">
        <ElDatePicker
          v-model="dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
        />
        <ElButton
          v-if="hasAuth('ai:metrics:read')"
          type="primary"
          :loading="loading"
          @click="reload"
          >查询 / 刷新</ElButton
        >
        <ElButton v-if="hasAuth('ai:price:create')" @click="priceVisible = true"
          >新增模型价格</ElButton
        >
      </div>
      <p class="hint"
        >最多查询 31 天，日期按服务端日历统计。队列为当前租户快照，不受日期筛选影响。</p
      >
    </ElCard>
    <ElAlert
      v-if="!hasAuth('ai:metrics:read')"
      title="需要“查询指标与成本”权限"
      type="info"
      :closable="false"
    />
    <template v-if="data">
      <ElAlert
        v-for="warning in warnings"
        :key="warning"
        :title="warning"
        type="warning"
        :closable="false"
      />
      <div class="cards">
        <ElCard v-for="card in cards" :key="card.title">
          <div class="hint">{{ card.title }}</div>
          <div class="value">{{ card.value }}</div>
          <div class="hint">{{ card.detail }}</div>
        </ElCard>
      </div>
      <ElCard>
        <ElDescriptions :column="3" border>
          <ElDescriptionsItem label="输入 / 输出 Token"
            >{{ number(data.model.inputTokens) }} /
            {{ number(data.model.outputTokens) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="首 Token 平均延迟">{{
            ms(data.model.averageTtftMs)
          }}</ElDescriptionsItem>
          <ElDescriptionsItem label="模型耗时 P50 / P99"
            >{{ ms(data.latency.p50Ms) }} / {{ ms(data.latency.p99Ms) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="取消 / 未结束模型调用"
            >{{ number(data.model.cancelled) }} /
            {{ number(data.model.unfinished) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="RAG 空结果 / 错误"
            >{{ number(data.rag.emptyCount) }} / {{ number(data.rag.errors) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="工具最终步骤失败率">{{
            percent(data.toolSteps.errorRate)
          }}</ElDescriptionsItem>
          <ElDescriptionsItem label="入库运行 / 取消待处理"
            >{{ number(data.queue.running) }} /
            {{ number(data.queue.cancelling) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="剩余分块 / 租约过期"
            >{{ number(data.queue.remainingChunks) }} /
            {{ number(data.queue.expiredLeases) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="窗口入库成功 / 失败 / 重试"
            >{{ number(data.queueHistory.succeeded) }} / {{ number(data.queueHistory.failed) }} /
            {{ number(data.queueHistory.retries) }}</ElDescriptionsItem
          >
          <ElDescriptionsItem label="窗口完成分块">{{
            number(data.queueHistory.completedChunks)
          }}</ElDescriptionsItem>
          <ElDescriptionsItem label="入库平均执行耗时">{{
            ms(data.queueHistory.averageExecutionMs)
          }}</ElDescriptionsItem>
          <ElDescriptionsItem label="首 Token 延迟 P95">{{
            ms(data.latency.ttftP95Ms)
          }}</ElDescriptionsItem>
          <ElDescriptionsItem label="近 15 分钟入库吞吐"
            >{{
              Number(data.queueThroughput.chunksPerSecond).toFixed(3)
            }}
            分块/秒</ElDescriptionsItem
          >
          <ElDescriptionsItem label="查询时间">{{ data.generatedAt }}</ElDescriptionsItem>
        </ElDescriptions>
        <p class="hint"
          >成本为已知用量的估算值；不同币种分别展示。取消不计入错误率，无样本显示 —。RAG
          命中表示返回有效分块，答案质量需单独评估。</p
        >
      </ElCard>
      <ElCard>
        <ElTabs v-model="tab">
          <ElTabPane label="模型统计" name="models">
            <ElTable :data="data.models" stripe>
              <ElTableColumn prop="provider" label="供应商" width="110" />
              <ElTableColumn prop="modelName" label="模型" min-width="170" />
              <ElTableColumn prop="operation" label="阶段" min-width="160" />
              <ElTableColumn prop="calls" label="调用数" width="90" />
              <ElTableColumn prop="failed" label="失败 / 超时" width="110" />
              <ElTableColumn label="平均延迟" width="120"
                ><template #default="{ row }">{{ ms(row.averageMs) }}</template></ElTableColumn
              >
              <ElTableColumn label="估算成本" min-width="160"
                ><template #default="{ row }">{{
                  cost(row.estimatedCost, row.currency)
                }}</template></ElTableColumn
              >
              <ElTableColumn prop="unknownCost" label="成本未知数" width="120" />
            </ElTable>
          </ElTabPane>
          <ElTabPane label="每日用量" name="daily">
            <ElTable :data="data.daily" stripe>
              <ElTableColumn prop="day" label="日期" />
              <ElTableColumn prop="calls" label="调用数" />
              <ElTableColumn prop="inputTokens" label="输入 Token" />
              <ElTableColumn prop="outputTokens" label="输出 Token" />
              <ElTableColumn label="估算成本"
                ><template #default="{ row }">{{
                  cost(row.estimatedCost, row.currency)
                }}</template></ElTableColumn
              >
              <ElTableColumn prop="unknownCost" label="成本未知数" />
            </ElTable>
          </ElTabPane>
          <ElTabPane label="工具可靠性" name="tools">
            <ElTable :data="data.tools" stripe>
              <ElTableColumn prop="toolName" label="工具" />
              <ElTableColumn prop="attempts" label="尝试次数" />
              <ElTableColumn prop="failed" label="失败 / 超时" />
              <ElTableColumn prop="timeouts" label="超时" />
              <ElTableColumn prop="cancelled" label="取消" />
              <ElTableColumn label="错误率"
                ><template #default="{ row }">{{ percent(row.errorRate) }}</template></ElTableColumn
              >
              <ElTableColumn label="平均延迟"
                ><template #default="{ row }">{{ ms(row.averageMs) }}</template></ElTableColumn
              >
            </ElTable>
          </ElTabPane>
          <ElTabPane label="模型调用明细" name="calls">
            <div class="toolbar">
              <ElInput v-model="filters.runId" placeholder="runId" clearable />
              <ElInput v-model="filters.modelName" placeholder="模型名称（精确）" clearable />
              <ElSelect v-model="filters.outcome" placeholder="全部结果" clearable>
                <ElOption
                  v-for="value in ['SUCCESS', 'ERROR', 'TIMEOUT', 'CANCELLED', 'IN_PROGRESS']"
                  :key="value"
                  :label="value"
                  :value="value"
                />
              </ElSelect>
              <ElButton :loading="callsLoading" @click="searchCalls">筛选</ElButton>
            </div>
            <ElTable :data="calls" v-loading="callsLoading" stripe>
              <ElTableColumn prop="startedAt" label="开始时间" min-width="180" />
              <ElTableColumn prop="runId" label="runId" min-width="180" show-overflow-tooltip />
              <ElTableColumn prop="jobId" label="入库任务" min-width="150" />
              <ElTableColumn prop="modelName" label="模型" min-width="160" />
              <ElTableColumn prop="operation" label="阶段" min-width="160" />
              <ElTableColumn prop="outcome" label="结果" width="120" />
              <ElTableColumn label="耗时"
                ><template #default="{ row }">{{ ms(row.durationMs) }}</template></ElTableColumn
              >
              <ElTableColumn prop="inputTokens" label="输入 Token" width="110" />
              <ElTableColumn prop="outputTokens" label="输出 Token" width="110" />
              <ElTableColumn label="估算成本" min-width="160"
                ><template #default="{ row }">{{
                  cost(row.estimatedCost, row.currency)
                }}</template></ElTableColumn
              >
              <ElTableColumn prop="costStatus" label="成本状态" min-width="160" />
              <ElTableColumn prop="errorCategory" label="错误类型" min-width="150" />
            </ElTable>
            <ElPagination
              v-model:current-page="callPage"
              :page-size="20"
              :total="callTotal"
              layout="total, prev, pager, next"
              @current-change="loadCalls"
            />
          </ElTabPane>
          <ElTabPane label="价格版本" name="prices">
            <p class="hint"
              >单价单位：每百万
              Token。新版本立即生效；历史调用价格保持不变。未配置价格的模型显示未知成本。</p
            >
            <ElTable :data="prices" stripe>
              <ElTableColumn prop="provider" label="供应商" />
              <ElTableColumn prop="modelName" label="模型" min-width="170" />
              <ElTableColumn prop="currency" label="币种" />
              <ElTableColumn prop="inputPrice" label="输入单价" />
              <ElTableColumn prop="cachedInputPrice" label="缓存输入单价" />
              <ElTableColumn prop="outputPrice" label="输出单价" />
              <ElTableColumn prop="effectiveFrom" label="生效时间" min-width="180" />
              <ElTableColumn label="状态"
                ><template #default="{ row }">{{
                  Number(row.enabled) ? '价格版本' : '待配置模板'
                }}</template></ElTableColumn
              >
            </ElTable>
            <ElPagination
              v-model:current-page="pricePage"
              :page-size="20"
              :total="priceTotal"
              layout="total, prev, pager, next"
              @current-change="loadPrices"
            />
          </ElTabPane>
        </ElTabs>
      </ElCard>
    </template>
    <ElDialog v-model="priceVisible" title="新增模型价格版本" width="520px">
      <ElForm label-width="120px">
        <ElFormItem label="供应商"
          ><ElInput v-model="price.provider" placeholder="例如 openai"
        /></ElFormItem>
        <ElFormItem label="模型名称"
          ><ElInput v-model="price.modelName" placeholder="实际模型名称"
        /></ElFormItem>
        <ElFormItem label="币种"
          ><ElInput v-model="price.currency" maxlength="3" placeholder="USD / CNY"
        /></ElFormItem>
        <ElFormItem label="输入单价"
          ><ElInput v-model="price.inputPrice" placeholder="每百万 Token 单价"
        /></ElFormItem>
        <ElFormItem label="缓存输入单价"
          ><ElInput v-model="price.cachedInputPrice" placeholder="留空使用输入单价"
        /></ElFormItem>
        <ElFormItem label="输出单价"
          ><ElInput v-model="price.outputPrice" placeholder="Embedding 可填写 0"
        /></ElFormItem>
      </ElForm>
      <template #footer
        ><ElButton @click="priceVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="savePrice"
          >发布新版本</ElButton
        ></template
      >
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useAuth } from '@/hooks/core/useAuth'
  import { getAiMetrics, getModelCalls, getModelPrices, publishModelPrice } from '@/api/ai-metrics'
  import type { AiMetricsOverview, MetricRow, MetricValue } from '@/api/ai-metrics'
  defineOptions({ name: 'AiMetrics' })
  const { hasAuth } = useAuth()
  const dateText = (date: Date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  const start = new Date()
  start.setDate(start.getDate() - 6)
  const dates = ref<[string, string]>([dateText(start), dateText(new Date())])
  const data = ref<AiMetricsOverview>()
  const loading = ref(false),
    callsLoading = ref(false),
    saving = ref(false),
    priceVisible = ref(false)
  const tab = ref('models')
  const calls = ref<MetricRow[]>([]),
    prices = ref<MetricRow[]>([])
  const callPage = ref(1),
    callTotal = ref(0),
    pricePage = ref(1),
    priceTotal = ref(0)
  const filters = reactive({ runId: '', modelName: '', outcome: '' })
  const price = reactive({
    provider: 'openai',
    modelName: '',
    currency: 'USD',
    inputPrice: '',
    cachedInputPrice: '',
    outputPrice: ''
  })
  const number = (value?: MetricValue) => (value == null ? '—' : Number(value).toLocaleString())
  const percent = (value?: MetricValue) =>
    value == null ? '—' : `${(Number(value) * 100).toFixed(2)}%`
  const ms = (value?: MetricValue) => (value == null ? '—' : `${Number(value).toFixed(0)} ms`)
  const cost = (value?: MetricValue, currency?: MetricValue) =>
    value == null
      ? '未知'
      : `${String(value).replace(/(\.\d*?[1-9])0+$|\.0+$/, '$1')} ${currency ?? ''}`
  const toolError = computed(() => {
    const rows = data.value?.tools ?? []
    const failed = rows.reduce((sum, row) => sum + Number(row.failed ?? 0), 0)
    const total = rows.reduce(
      (sum, row) => sum + Number(row.failed ?? 0) + Number(row.succeeded ?? 0),
      0
    )
    return total ? failed / total : null
  })
  const cards = computed(() =>
    !data.value
      ? []
      : [
          {
            title: '已知估算成本',
            value:
              data.value.costs.map((row) => cost(row.estimatedCost, row.currency)).join(' / ') ||
              '未知 / 无样本',
            detail: `成本未知 ${number(data.value.model.unknownCost)} 次`
          },
          {
            title: '模型延迟 P95',
            value: ms(data.value.latency.p95Ms),
            detail: `平均 ${ms(data.value.model.averageMs)}`
          },
          {
            title: '模型成功率',
            value: percent(data.value.model.successRate),
            detail: `模型调用 ${number(data.value.model.calls)} 次`
          },
          {
            title: 'RAG 命中率',
            value: percent(data.value.rag.hitRate),
            detail: `有效命中 ${number(data.value.rag.hits)} 次`
          },
          {
            title: '工具尝试错误率',
            value: percent(toolError.value),
            detail: '失败与超时计入，取消排除'
          },
          {
            title: '等待入库任务',
            value: number(data.value.queue.pending),
            detail: `最老等待任务年龄 ${number(data.value.queue.oldestPendingAgeSeconds)} 秒`
          }
        ]
  )
  const warnings = computed(() => {
    if (!data.value) return []
    const result: string[] = [],
      d = data.value
    if (Number(d.model.unknownCost) > 0)
      result.push('存在成本未知的调用，请检查价格、供应商用量或尚未结束的调用。')
    if (
      Number(d.model.succeeded) + Number(d.model.failed) >= 20 &&
      Number(d.model.successRate) < 0.95
    )
      result.push('当前窗口模型成功率低于 95%。')
    if (Number(d.model.succeeded) + Number(d.model.failed) >= 20 && Number(d.latency.p95Ms) > 30000)
      result.push('当前窗口模型完成耗时 P95 超过 30 秒。')
    if (
      Number(d.rag.hits) + Number(d.rag.emptyCount) + Number(d.rag.errors) >= 20 &&
      Number(d.rag.errorRate) > 0.05
    )
      result.push('当前窗口 RAG 检索错误率超过 5%。')
    if (
      d.tools.reduce((sum, row) => sum + Number(row.succeeded) + Number(row.failed), 0) >= 20 &&
      Number(toolError.value) > 0.1
    )
      result.push('当前窗口工具尝试错误率超过 10%。')
    if (Number(d.queue.oldestPendingAgeSeconds) > 300)
      result.push('有等待入库任务年龄超过 5 分钟；重试任务显示从首次创建起的年龄。')
    if (Number(d.queue.expiredLeases) > 0)
      result.push('存在租约过期的运行任务，请检查入库 Worker。')
    if (Number(d.queue.unknownChunkJobs) > 0)
      result.push('部分活动入库任务的分块总数未知，剩余分块统计不完整。')
    if (Number(d.instanceWriteFailures) > 0)
      result.push('当前响应实例发生过观测写入失败，统计可能不完整，请检查服务日志。')
    return result
  })
  let callSequence = 0
  async function loadCalls() {
    const sequence = ++callSequence
    callsLoading.value = true
    try {
      const page = await getModelCalls({
        ...filters,
        startDate: dates.value[0],
        endDate: dates.value[1],
        current: callPage.value,
        size: 20
      })
      if (sequence === callSequence) {
        calls.value = page.records
        callTotal.value = Number(page.total)
      }
    } catch {
      if (sequence === callSequence) {
        calls.value = []
        callTotal.value = 0
      }
    } finally {
      if (sequence === callSequence) callsLoading.value = false
    }
  }
  function searchCalls() {
    callPage.value = 1
    void loadCalls()
  }
  async function loadPrices() {
    try {
      const page = await getModelPrices(pricePage.value)
      prices.value = page.records
      priceTotal.value = Number(page.total)
    } catch {
      prices.value = []
      priceTotal.value = 0
    }
  }
  async function reload() {
    if (!hasAuth('ai:metrics:read') || loading.value) return
    loading.value = true
    data.value = undefined
    try {
      data.value = await getAiMetrics(...dates.value)
      searchCalls()
      await loadPrices()
    } catch {
      /* Shared request handling displays the error; never display stale statistics. */
    } finally {
      loading.value = false
    }
  }
  async function savePrice() {
    const numeric = /^(?:0|[1-9]\d*)(?:\.\d{1,8})?$/
    if (
      !price.provider.trim() ||
      !price.modelName.trim() ||
      !/^[a-zA-Z]{3}$/.test(price.currency) ||
      !numeric.test(price.inputPrice) ||
      !numeric.test(price.outputPrice) ||
      (price.cachedInputPrice && !numeric.test(price.cachedInputPrice))
    ) {
      ElMessage.warning('请填写供应商、模型、三位币种和非负单价（最多 8 位小数）')
      return
    }
    try {
      await ElMessageBox.confirm('新价格立即生效，历史调用不会重新计价。确认发布？', '发布模型价格')
    } catch {
      return
    }
    saving.value = true
    try {
      await publishModelPrice({ ...price, cachedInputPrice: price.cachedInputPrice || undefined })
      priceVisible.value = false
      ElMessage.success('价格版本已发布')
      pricePage.value = 1
      await loadPrices()
    } catch {
      /* Shared request handler displays the error. */
    } finally {
      saving.value = false
    }
  }
  onMounted(reload)
</script>

<style scoped>
  .metrics-page {
    display: grid;
    gap: 16px;
  }
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
  }
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select) {
    width: 220px;
  }
  .cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .hint {
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .value {
    margin: 12px 0;
    font-size: 24px;
    font-weight: 600;
    overflow-wrap: anywhere;
  }
  .el-pagination {
    margin-top: 16px;
  }
</style>

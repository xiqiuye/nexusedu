<template>
  <div class="operations-page">
    <ElCard>
      <div class="toolbar">
        <ElSelect v-model="type" clearable placeholder="全部类型" @change="resetPage"
          ><ElOption
            v-for="item in types"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        /></ElSelect>
        <ElInput
          v-model="key"
          placeholder="资源标识，模型如 balanced"
          clearable
          @keyup.enter="resetPage"
        />
        <ElButton :loading="loading" @click="resetPage">查询</ElButton>
        <ElButton v-if="hasAuth('ai:config:write')" type="primary" @click="openDraft()"
          >新增版本</ElButton
        >
        <ElButton v-if="hasAuth('ai:config:publish')" @click="openRollout()"
          >发布 / 灰度 / 回滚</ElButton
        >
        <ElButton v-if="hasAuth('ai:policy:write')" @click="openPolicy()">权限与控制策略</ElButton>
        <ElButton @click="router.push('/system/role')">角色权限配置</ElButton>
        <ElButton @click="router.push('/ai/metrics')">价格与调用账本</ElButton>
      </div>
      <p
        >版本内容不可修改，调整时新建版本。作者提交后须由另一位审批人批准，再配置稳定版和灰度比例。灰度按用户固定分组；已启动任务继续使用原版本。</p
      >
      <ElTabs v-model="tab" @tab-change="resetPage">
        <ElTabPane label="模型 / 工具 / 提示词版本" name="versions" />
        <ElTabPane label="当前发布" name="rollouts" />
        <ElTabPane label="权限、限流与成本" name="policies" />
        <ElTabPane label="限额使用量" name="usage" />
      </ElTabs>
      <ElTable :data="rows" v-loading="loading">
        <ElTableColumn prop="resourceType" label="类型" width="95" />
        <ElTableColumn prop="resourceKey" label="资源" min-width="130" />
        <template v-if="tab === 'versions'">
          <ElTableColumn prop="version" label="版本" width="130" />
          <ElTableColumn prop="status" label="审批状态" width="125" />
          <ElTableColumn prop="createBy" label="作者" width="100" />
          <ElTableColumn prop="reviewedBy" label="审批人" width="100" />
          <ElTableColumn prop="reviewNote" label="审批意见" show-overflow-tooltip />
          <ElTableColumn label="配置" width="80"
            ><template #default="{ row }"
              ><ElButton link @click="showDetails(row)">查看</ElButton></template
            ></ElTableColumn
          >
        </template>
        <template v-else-if="tab === 'rollouts'">
          <ElTableColumn prop="stableVersionId" label="稳定版 ID" min-width="190" />
          <ElTableColumn prop="candidateVersionId" label="候选版 ID" min-width="190" />
          <ElTableColumn prop="canaryPercent" label="灰度 %" width="100" />
          <ElTableColumn prop="modifyBy" label="发布人" width="110" />
        </template>
        <template v-else-if="tab === 'policies'">
          <ElTableColumn prop="enabled" label="启用" width="70" />
          <ElTableColumn prop="permissionCode" label="执行权限" min-width="180" />
          <ElTableColumn prop="requestsPerMinute" label="请求 / 分钟" width="120" />
          <ElTableColumn prop="dailyBudget" label="日预算" width="120" />
          <ElTableColumn prop="reservePerCall" label="单次预占" width="120" />
          <ElTableColumn prop="currency" label="币种" width="80" />
        </template>
        <template v-else>
          <ElTableColumn prop="windowKey" label="时间窗口" min-width="190" />
          <ElTableColumn prop="requestCount" label="已用请求" width="120" />
          <ElTableColumn prop="chargedCost" label="已结算 + 未结算预占" min-width="180" />
        </template>
        <ElTableColumn v-if="tab !== 'usage'" label="操作" min-width="230">
          <template #default="{ row }">
            <template v-if="tab === 'versions'">
              <ElButton v-if="hasAuth('ai:config:write')" link @click="openDraft(row)"
                >复制新版本</ElButton
              >
              <ElButton
                v-if="row.status === 'DRAFT' && hasAuth('ai:config:write')"
                link
                type="primary"
                @click="review(row, 'submit')"
                >提交审批</ElButton
              >
              <template v-if="row.status === 'PENDING' && hasAuth('ai:config:approve')">
                <ElButton link type="success" @click="review(row, 'approve')">批准</ElButton>
                <ElButton link type="danger" @click="review(row, 'reject')">拒绝</ElButton>
              </template>
              <ElButton
                v-if="row.status === 'APPROVED' && hasAuth('ai:config:publish')"
                link
                @click="openRollout(row)"
                >发布</ElButton
              >
            </template>
            <ElButton
              v-else-if="tab === 'rollouts' && hasAuth('ai:config:publish')"
              link
              @click="openRollout(row)"
              >调整发布</ElButton
            >
            <ElButton
              v-else-if="tab === 'policies' && hasAuth('ai:policy:write')"
              link
              @click="openPolicy(row)"
              >修改策略</ElButton
            >
          </template>
        </ElTableColumn>
      </ElTable>
      <ElPagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="reload"
      />
    </ElCard>
    <ElDialog v-model="draftVisible" title="创建不可变版本" width="680px">
      <ElForm label-width="110px">
        <ElFormItem label="资源类型"
          ><ElSelect v-model="draft.resourceType" @change="resetConfig"
            ><ElOption
              v-for="item in types"
              :key="item.value"
              :label="item.label"
              :value="item.value" /></ElSelect
        ></ElFormItem>
        <ElFormItem label="资源标识"
          ><ElInput
            v-model="draft.resourceKey"
            placeholder="balanced / query_knowledge / AI_PLAN / AI_COMPOSE"
        /></ElFormItem>
        <ElFormItem label="版本号"><ElInput v-model="draft.version" /></ElFormItem>
        <ElFormItem label="启用"><ElSwitch v-model="config.enabled" /></ElFormItem>
        <template v-if="draft.resourceType === 'MODEL'">
          <ElFormItem label="厂商"
            ><ElSelect v-model="config.provider"
              ><ElOption label="OpenAI 兼容" value="openai" /><ElOption
                label="DeepSeek"
                value="deepseek" /></ElSelect
          ></ElFormItem>
          <ElFormItem label="模型名称"><ElInput v-model="config.modelName" /></ElFormItem>
          <ElFormItem label="HTTPS 地址"><ElInput v-model="config.baseUrl" /></ElFormItem>
          <ElFormItem label="密钥环境变量"
            ><ElInput v-model="config.apiKeyEnv" placeholder="OPENAI_API_KEY"
          /></ElFormItem>
          <ElFormItem label="输出 Token 上限"
            ><ElInputNumber v-model="config.maxOutputTokens" :min="1" :max="32768"
          /></ElFormItem>
        </template>
        <template v-else-if="draft.resourceType === 'TOOL'">
          <ElFormItem label="领域"
            ><ElSelect v-model="config.domain"
              ><ElOption label="学生查询" value="student" /><ElOption
                label="知识检索"
                value="knowledge" /></ElSelect
          ></ElFormItem>
          <ElFormItem label="描述"
            ><ElInput v-model="config.description" type="textarea" :rows="3"
          /></ElFormItem>
          <ElFormItem label="参数说明"
            ><ElInput v-model="config.argumentSpecification" type="textarea" :rows="4"
          /></ElFormItem>
          <ElFormItem label="关键词"><ElInput v-model="config.keywords" /></ElFormItem>
          <p>仅支持已部署的 query_student / query_knowledge，实际权限与参数仍由服务端校验。</p>
        </template>
        <ElFormItem v-else label="模板内容"
          ><ElInput v-model="config.content" type="textarea" :rows="12"
        /></ElFormItem>
      </ElForm>
      <template #footer
        ><ElButton @click="draftVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitDraft"
          >保存草稿</ElButton
        ></template
      >
    </ElDialog>
    <ElDialog v-model="rolloutVisible" title="发布 / 灰度 / 回滚" width="600px">
      <ElForm label-width="110px">
        <ElFormItem label="类型"
          ><ElSelect v-model="rollout.resourceType" @change="loadApproved"
            ><ElOption
              v-for="item in types"
              :key="item.value"
              :label="item.label"
              :value="item.value" /></ElSelect
        ></ElFormItem>
        <ElFormItem label="资源标识"
          ><ElInput v-model="rollout.resourceKey" @change="loadApproved"
        /></ElFormItem>
        <ElFormItem label="稳定版"
          ><ElSelect v-model="rollout.stableVersionId" filterable
            ><ElOption
              v-for="item in approved"
              :key="item.id"
              :label="`${item.version} (${item.id})`"
              :value="item.id" /></ElSelect
        ></ElFormItem>
        <ElFormItem label="候选版"
          ><ElSelect v-model="rollout.candidateVersionId" clearable filterable
            ><ElOption
              v-for="item in approved"
              :key="item.id"
              :label="`${item.version} (${item.id})`"
              :value="item.id" /></ElSelect
        ></ElFormItem>
        <ElFormItem label="灰度比例 %"
          ><ElSlider v-model="rollout.canaryPercent" :max="100" show-input
        /></ElFormItem>
      </ElForm>
      <p>只有已批准版本可发布。将稳定版切回先前版本即为回滚；比例为 0 时仅使用稳定版。</p>
      <template #footer
        ><ElButton @click="rolloutVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitRollout">发布</ElButton></template
      >
    </ElDialog>
    <ElDialog v-model="policyVisible" title="权限、限流与成本策略" width="630px">
      <ElForm label-width="120px">
        <ElFormItem label="类型"
          ><ElSelect v-model="policy.resourceType"
            ><ElOption
              v-for="item in types"
              :key="item.value"
              :label="item.label"
              :value="item.value" /></ElSelect
        ></ElFormItem>
        <ElFormItem label="资源标识"
          ><ElInput v-model="policy.resourceKey" placeholder="* 表示该类型所有资源"
        /></ElFormItem>
        <ElFormItem label="启用"><ElSwitch v-model="policy.enabled" /></ElFormItem>
        <ElFormItem label="所需执行权限"
          ><ElInput
            v-model="policy.permissionCode"
            placeholder="空表示已登录用户；如 knowledge:chunk:read"
        /></ElFormItem>
        <ElFormItem label="每分钟请求"
          ><ElInputNumber v-model="policy.requestsPerMinute" :min="0" :max="1000000"
        /></ElFormItem>
        <template v-if="policy.resourceType === 'MODEL'">
          <ElFormItem label="币种"><ElInput v-model="policy.currency" maxlength="3" /></ElFormItem>
          <ElFormItem label="每日预算"
            ><ElInput v-model="policy.dailyBudget" placeholder="留空不限制，使用十进制金额"
          /></ElFormItem>
          <ElFormItem label="单次预占"><ElInput v-model="policy.reservePerCall" /></ElFormItem>
        </template>
      </ElForm>
      <p
        >频率为 0 表示不限制。模型可用别名 balanced，Embedding 用
        provider:modelName。通配符与具体策略同时生效。启用预算须先配置同币种模型价格；未知用量保留预占。预占金额需覆盖预计单次费用，实际费用超过预占时会阻止后续调用。</p
      >
      <template #footer
        ><ElButton @click="policyVisible = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="submitPolicy"
          >保存策略</ElButton
        ></template
      >
    </ElDialog>
    <ElDialog v-model="detailsVisible" title="版本详情" width="750px">
      <pre>{{ JSON.stringify(details, null, 2) }}</pre>
    </ElDialog>
  </div>
</template>
<script setup lang="ts">
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useAuth } from '@/hooks/core/useAuth'
  import {
    operationList,
    draftVersion,
    reviewVersion,
    saveRollout,
    saveResourcePolicy
  } from '@/api/ai-operations'
  import type { ManagementRow } from '@/api/management'
  defineOptions({ name: 'AiConfiguration' })
  const { hasAuth } = useAuth(),
    router = useRouter()
  const types = [
    { value: 'MODEL', label: '模型' },
    { value: 'TOOL', label: '工具' },
    { value: 'PROMPT', label: '提示词' }
  ]
  const type = ref(''),
    key = ref(''),
    tab = ref('versions'),
    page = ref(1),
    total = ref(0),
    rows = ref<ManagementRow[]>([]),
    loading = ref(false),
    saving = ref(false)
  const draftVisible = ref(false),
    rolloutVisible = ref(false),
    policyVisible = ref(false),
    detailsVisible = ref(false),
    details = ref<ManagementRow>()
  const draft = reactive({ resourceType: 'MODEL', resourceKey: '', version: '' }),
    config = ref<Record<string, any>>({})
  const rollout = reactive({
    resourceType: 'MODEL',
    resourceKey: '',
    stableVersionId: '',
    candidateVersionId: undefined as string | undefined,
    canaryPercent: 0
  })
  const approved = ref<ManagementRow[]>([])
  function showDetails(row: ManagementRow) {
    details.value = row
    detailsVisible.value = true
  }
  const policy = reactive({
    resourceType: 'MODEL',
    resourceKey: '*',
    enabled: true,
    requestsPerMinute: 0,
    dailyBudget: '',
    reservePerCall: '0',
    currency: 'USD',
    permissionCode: ''
  })
  let requestGeneration = 0
  async function reload() {
    if (!hasAuth('ai:config:read')) return
    const currentGeneration = ++requestGeneration
    loading.value = true
    try {
      const result = await operationList('config', tab.value, {
        type: type.value,
        key: key.value,
        current: page.value,
        size: 20
      })
      if (currentGeneration !== requestGeneration) return
      rows.value = result.records
      total.value = result.total
    } finally {
      if (currentGeneration === requestGeneration) loading.value = false
    }
  }
  function resetPage() {
    page.value = 1
    reload()
  }
  function resetConfig() {
    config.value =
      draft.resourceType === 'MODEL'
        ? {
            provider: 'openai',
            modelName: '',
            baseUrl: 'https://api.openai.com/v1',
            apiKeyEnv: 'OPENAI_API_KEY',
            maxOutputTokens: 4096,
            enabled: true
          }
        : draft.resourceType === 'TOOL'
          ? {
              domain: 'knowledge',
              description: '',
              argumentSpecification: '',
              keywords: '',
              enabled: true
            }
          : { content: '', enabled: true }
  }
  function openDraft(row?: ManagementRow) {
    Object.assign(draft, {
      resourceType: row?.resourceType ?? (type.value || 'MODEL'),
      resourceKey: row?.resourceKey ?? key.value,
      version: ''
    })
    resetConfig()
    if (row) config.value = structuredClone(row.config)
    draftVisible.value = true
  }
  async function execute(action: () => Promise<unknown>, close: () => void) {
    saving.value = true
    try {
      await action()
      close()
      ElMessage.success('已保存')
      await reload()
    } finally {
      saving.value = false
    }
  }
  async function submitDraft() {
    if (!draft.resourceKey.trim() || !draft.version.trim()) {
      ElMessage.warning('填写资源标识与版本号')
      return
    }
    await execute(
      () => draftVersion({ ...draft, config: config.value }),
      () => {
        draftVisible.value = false
      }
    )
  }
  async function review(row: ManagementRow, action: string) {
    let note = ''
    if (action !== 'submit') {
      try {
        note = (
          await ElMessageBox.prompt(
            '请输入审批意见（作者不能审批自己的版本）',
            action === 'approve' ? '批准版本' : '拒绝版本',
            { inputValidator: (value) => !!value?.trim() || '请填写审批意见' }
          )
        ).value
      } catch {
        return
      }
    }
    await reviewVersion(row.id, action, note)
    await reload()
  }
  let approvedGeneration = 0
  async function loadApproved() {
    const currentGeneration = ++approvedGeneration
    approved.value = []
    if (!rollout.resourceKey) return
    for (let current = 1; ; current++) {
      const result = await operationList('config', 'versions', {
        type: rollout.resourceType,
        key: rollout.resourceKey,
        current,
        size: 200
      })
      if (currentGeneration !== approvedGeneration) return
      approved.value.push(...result.records.filter((row) => row.status === 'APPROVED'))
      if (current * 200 >= result.total) break
    }
  }
  async function openRollout(row?: ManagementRow) {
    Object.assign(rollout, {
      resourceType: row?.resourceType ?? (type.value || 'MODEL'),
      resourceKey: row?.resourceKey ?? key.value,
      stableVersionId: row?.stableVersionId ?? row?.id ?? '',
      candidateVersionId: row?.candidateVersionId ?? undefined,
      canaryPercent: row?.canaryPercent ?? 0
    })
    rolloutVisible.value = true
    await loadApproved()
  }
  async function submitRollout() {
    if (!rollout.stableVersionId) {
      ElMessage.warning('请选择稳定版')
      return
    }
    await execute(
      () => saveRollout({ ...rollout, candidateVersionId: rollout.candidateVersionId || null }),
      () => {
        rolloutVisible.value = false
      }
    )
  }
  function openPolicy(row?: ManagementRow) {
    Object.assign(policy, {
      resourceType: row?.resourceType ?? (type.value || 'MODEL'),
      resourceKey: row?.resourceKey ?? (key.value || '*'),
      enabled: row ? !!row.enabled : true,
      requestsPerMinute: row?.requestsPerMinute ?? 0,
      dailyBudget: row?.dailyBudget ?? '',
      reservePerCall: row?.reservePerCall ?? '0',
      currency: row?.currency ?? 'USD',
      permissionCode: row?.permissionCode ?? ''
    })
    policyVisible.value = true
  }
  async function submitPolicy() {
    await execute(
      () =>
        saveResourcePolicy({
          ...policy,
          dailyBudget: policy.resourceType === 'MODEL' ? policy.dailyBudget || null : null,
          reservePerCall: policy.resourceType === 'MODEL' ? policy.reservePerCall : '0'
        }),
      () => {
        policyVisible.value = false
      }
    )
  }
  onMounted(reload)
</script>
<style scoped>
  .operations-page {
    display: grid;
    gap: 16px;
    padding: 16px;
  }
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  .toolbar .el-select,
  .toolbar .el-input {
    width: 230px;
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

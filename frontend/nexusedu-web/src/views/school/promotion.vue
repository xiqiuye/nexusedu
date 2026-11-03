<template>
  <main class="school-page">
    <header class="page-heading">
      <div><span class="eyebrow">ACADEMIC YEAR TRANSITION</span><h1>学年升班</h1></div>
      <div class="heading-actions">
        <el-input v-model="auditId" placeholder="批次编号" @keyup.enter="loadAudit" />
        <el-button :disabled="!auditId" @click="loadAudit">核对批次</el-button>
        <el-button :disabled="!previewRows.length" :loading="saving" type="primary" @click="confirm"
          >确认升班</el-button
        >
      </div>
    </header>
    <section class="setup-row">
      <el-select v-model="form.campusId" placeholder="选择校区" @change="clearPreview">
        <el-option v-for="item in campuses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="form.sourceAcademicYearId" placeholder="来源学年" @change="clearPreview">
        <el-option v-for="item in years" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <span class="arrow">→</span>
      <el-select v-model="form.targetAcademicYearId" placeholder="目标学年" @change="clearPreview">
        <el-option
          v-for="item in years"
          :key="item.id"
          :label="item.name"
          :value="item.id"
          :disabled="item.id === form.sourceAcademicYearId"
        />
      </el-select>
      <el-select v-model="form.grade" placeholder="来源年级" @change="clearPreview">
        <el-option
          v-for="grade in 6"
          :key="grade"
          :label="`小学${gradeLabel(grade)}年级`"
          :value="grade"
        />
      </el-select>
      <el-date-picker
        v-model="form.effectiveOn"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="生效日期"
      />
      <el-button type="primary" plain :loading="loading" @click="preview">生成预览</el-button>
    </section>

    <section v-if="previewRows.length" class="table-shell">
      <div class="table-meta"
        ><strong>{{ previewRows.length }} 位学生待处理</strong
        ><span>逐人确认升年级、留级或毕业安排</span></div
      >
      <el-table :data="previewRows" row-key="studentId" stripe>
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="110" />
        <el-table-column label="当前班级" min-width="140"
          ><template #default="{ row }">{{ row.className || '未分班' }}</template></el-table-column
        >
        <el-table-column label="处理方式" min-width="150">
          <template #default="{ row }">
            <el-select v-model="row.action" @change="changeAction(row)">
              <el-option label="升入下一年级" value="PROMOTE" />
              <el-option label="留级" value="RETAIN" />
              <el-option v-if="form.grade === 6" label="毕业" value="GRADUATE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="目标年级与班级" min-width="240">
          <template #default="{ row }">
            <div v-if="row.action !== 'GRADUATE'" class="target-fields">
              <el-select v-model="row.targetGrade" @change="row.targetClassId = ''">
                <el-option
                  v-for="grade in 6"
                  :key="grade"
                  :label="`小学${gradeLabel(grade)}年级`"
                  :value="grade"
                />
              </el-select>
              <el-select v-model="row.targetClassId" placeholder="目标班级">
                <el-option
                  v-for="item in classOptions(row.targetGrade)"
                  :key="item.id"
                  :label="item.className"
                  :value="item.id"
                />
              </el-select>
            </div>
            <span v-else class="graduate-label">毕业，不建立新班级</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180"
          ><template #default="{ row }"
            ><el-input v-model="row.reason" placeholder="可选" /></template
        ></el-table-column>
      </el-table>
    </section>
    <el-empty v-else-if="searched" description="来源年级没有可处理的在读学生" />
    <section v-if="audit" class="audit-shell">
      <div class="table-meta"
        ><strong>升班批次 #{{ audit.id }}</strong
        ><span
          >{{ audit.sourceAcademicYear }} → {{ audit.targetAcademicYear }} ·
          {{ audit.confirmedAt }}</span
        ></div
      >
      <el-table :data="audit.items || []" row-key="id" stripe>
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="studentName" label="姓名" min-width="120" />
        <el-table-column label="处理结果" width="140"
          ><template #default="{ row }">{{ actionLabel(row.action) }}</template></el-table-column
        >
        <el-table-column label="目标安排" min-width="180"
          ><template #default="{ row }">{{
            row.action === 'GRADUATE'
              ? '毕业'
              : `小学${gradeLabel(row.targetGrade)}年级${row.targetClass || ''}`
          }}</template></el-table-column
        >
        <el-table-column prop="reason" label="备注" min-width="180" />
        <el-table-column prop="targetEnrollmentId" label="目标学籍 ID" min-width="140" />
      </el-table>
    </section>
  </main>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { SchoolOption, studentApi } from '@/api/students'
  const campuses = ref<SchoolOption[]>([])
  const years = ref<SchoolOption[]>([])
  const targetClasses = ref<Record<string, any>[]>([])
  const previewRows = ref<Record<string, any>[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const searched = ref(false)
  const auditId = ref('')
  const audit = ref<Record<string, any> | null>(null)
  const form = reactive<Record<string, any>>({
    campusId: '',
    sourceAcademicYearId: '',
    targetAcademicYearId: '',
    grade: 1,
    effectiveOn: new Date().toISOString().slice(0, 10)
  })
  const gradeLabel = (grade: number) => ['', '一', '二', '三', '四', '五', '六'][grade] || grade
  const actionLabel = (action: string) =>
    ({ PROMOTE: '升年级', RETAIN: '留级', GRADUATE: '毕业' })[action] || action
  function clearPreview() {
    previewRows.value = []
    searched.value = false
  }
  function classOptions(grade: number) {
    return targetClasses.value.filter((item) => Number(item.grade) === Number(grade))
  }
  function changeAction(row: Record<string, any>) {
    row.targetGrade =
      row.action === 'RETAIN'
        ? Number(form.grade)
        : Number(form.grade) < 6
          ? Number(form.grade) + 1
          : 6
    row.targetClassId = ''
  }
  async function preview() {
    if (!form.campusId || !form.sourceAcademicYearId || !form.targetAcademicYearId)
      return ElMessage.warning('请选择校区、来源学年和目标学年')
    loading.value = true
    try {
      const result = await studentApi.previewPromotion({
        campusId: form.campusId,
        sourceAcademicYearId: form.sourceAcademicYearId,
        targetAcademicYearId: form.targetAcademicYearId,
        grade: form.grade
      })
      targetClasses.value = result.targetClasses || []
      previewRows.value = (result.students || []).map((student: Record<string, any>) => {
        const action = Number(form.grade) === 6 ? 'GRADUATE' : 'PROMOTE'
        const targetGrade = action === 'GRADUATE' ? null : Number(form.grade) + 1
        const candidates = classOptions(targetGrade || 0)
        return {
          ...student,
          action,
          targetGrade,
          targetClassId: candidates[0]?.id || '',
          reason: ''
        }
      })
      searched.value = true
    } finally {
      loading.value = false
    }
  }
  async function confirm() {
    if (previewRows.value.some((row) => row.action !== 'GRADUATE' && !row.targetClassId))
      return ElMessage.warning('请为每位升班或留级学生选择目标班级')
    try {
      await ElMessageBox.confirm(
        `将处理 ${previewRows.value.length} 位学生，并写入目标学年的学籍记录。操作会保留来源学年历史。`,
        '确认升班',
        { type: 'warning' }
      )
    } catch {
      return
    }
    saving.value = true
    try {
      const result = await studentApi.confirmPromotion({
        ...form,
        students: previewRows.value.map(
          ({ studentId, action, targetGrade, targetClassId, reason }) => ({
            studentId,
            action,
            targetGrade,
            targetClassId,
            reason
          })
        )
      })
      ElMessage.success(`升班批次 ${result.batchId} 已确认，共处理 ${result.processed} 人`)
      auditId.value = result.batchId
      await loadAudit()
      previewRows.value = []
      searched.value = false
    } finally {
      saving.value = false
    }
  }
  async function loadAudit() {
    if (!auditId.value) return
    audit.value = await studentApi.promotionBatch(auditId.value)
  }
  onMounted(async () => {
    ;[campuses.value, years.value] = await Promise.all([studentApi.campuses(), studentApi.years()])
    form.campusId = campuses.value[0]?.id || ''
    form.sourceAcademicYearId =
      years.value.find((item) => item.current)?.id || years.value[0]?.id || ''
    form.targetAcademicYearId =
      years.value.find((item) => item.id !== form.sourceAcademicYearId)?.id || ''
  })
</script>

<style scoped>
  .school-page {
    padding: 24px;
    color: var(--el-text-color-primary);
  }
  .page-heading {
    display: flex;
    align-items: end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
  }
  .heading-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .heading-actions .el-input {
    width: 150px;
  }
  .eyebrow {
    color: var(--el-color-primary);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
  }
  h1 {
    margin: 4px 0 0;
    font-size: 24px;
    font-weight: 650;
  }
  .setup-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;
    border-block: 1px solid var(--el-border-color-lighter);
  }
  .setup-row .el-select {
    width: 180px;
  }
  .arrow {
    color: var(--el-text-color-secondary);
  }
  .table-shell {
    padding-top: 18px;
  }
  .audit-shell {
    padding-top: 24px;
  }
  .table-meta {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 0 0 12px;
  }
  .table-meta span,
  .graduate-label {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .target-fields {
    display: grid;
    grid-template-columns: 120px minmax(120px, 1fr);
    gap: 8px;
  }
  @media (max-width: 700px) {
    .school-page {
      padding: 16px;
    }
    .page-heading {
      align-items: start;
      flex-direction: column;
    }
    .heading-actions {
      width: 100%;
      flex-wrap: wrap;
    }
    .setup-row .el-select,
    .setup-row .el-date-editor {
      width: 100%;
    }
    .table-meta {
      flex-direction: column;
    }
  }
</style>

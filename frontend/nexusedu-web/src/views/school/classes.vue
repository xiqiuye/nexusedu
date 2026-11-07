<template>
  <main class="school-page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">SCHOOL ORGANIZATION</span>
        <h1>班级管理</h1>
      </div>
      <div class="heading-actions">
        <el-button @click="router.push('/organization/promotion')">学年升班</el-button>
        <el-button type="primary" @click="openCreate">新建班级</el-button>
      </div>
    </header>

    <section class="filter-row">
      <el-select
        v-model="filters.academicYearId"
        clearable
        placeholder="全部学年"
        @change="loadClasses"
      >
        <el-option v-for="item in years" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.campusId" clearable placeholder="全部校区" @change="loadClasses">
        <el-option v-for="item in campuses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.grade" clearable placeholder="全部年级" @change="loadClasses">
        <el-option
          v-for="grade in 6"
          :key="grade"
          :label="`小学${gradeLabel(grade)}年级`"
          :value="grade"
        />
      </el-select>
      <span class="result-count">{{ filteredClasses.length }} 个班级</span>
    </section>

    <section class="table-shell">
      <el-table v-loading="loading" :data="filteredClasses" row-key="id" stripe>
        <el-table-column label="班级" min-width="190">
          <template #default="{ row }">
            <button class="class-link" @click="openDetail(row)">
              <strong>小学{{ gradeLabel(row.grade) }}年级{{ row.className }}</strong>
              <span>{{ row.academicYear }} · {{ row.campusName }}</span>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="学生人数" width="110" align="center">
          <template #default="{ row }">
            <span
              >{{ row.studentCount || 0
              }}<small v-if="row.capacity"> / {{ row.capacity }}</small></span
            >
          </template>
        </el-table-column>
        <el-table-column
          prop="homeroomTeacher"
          label="班主任"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.homeroomTeacher || '未安排' }}</template>
        </el-table-column>
        <el-table-column prop="teachers" label="任课教师" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.teachers || '未安排' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="
                row.status === 'ACTIVE' ? 'success' : row.status === 'ARCHIVED' ? 'info' : 'warning'
              "
              effect="light"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">名册</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown @command="(status: string) => setStatus(row, status)">
              <el-button link>更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== 'ACTIVE'" command="ACTIVE"
                    >启用</el-dropdown-item
                  >
                  <el-dropdown-item v-if="row.status === 'ACTIVE'" command="INACTIVE"
                    >停用</el-dropdown-item
                  >
                  <el-dropdown-item v-if="row.status !== 'ARCHIVED'" command="ARCHIVED"
                    >归档</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前筛选下没有班级" /></template>
      </el-table>
    </section>

    <el-drawer
      v-model="detailVisible"
      :title="detail?.className || '班级名册'"
      size="min(760px, 94vw)"
    >
      <template v-if="detail">
        <div class="detail-summary">
          <div
            ><span>学年</span><strong>{{ detail.academicYear }}</strong></div
          >
          <div
            ><span>校区</span><strong>{{ detail.campusName }}</strong></div
          >
          <div
            ><span>年级</span><strong>小学{{ gradeLabel(detail.grade) }}年级</strong></div
          >
          <div
            ><span>在读人数</span
            ><strong
              >{{ detail.studentCount || 0
              }}{{ detail.capacity ? ` / ${detail.capacity}` : '' }}</strong
            ></div
          >
          <div
            ><span>班主任</span><strong>{{ detail.homeroomTeacher || '未安排' }}</strong></div
          >
          <div
            ><span>任课教师</span><strong>{{ detail.teachers || '未安排' }}</strong></div
          >
        </div>
        <div class="roster-toolbar">
          <h2>班级人员</h2>
          <el-button :disabled="!detail.id" @click="rosterInput?.click()">导入名册</el-button>
          <input ref="rosterInput" hidden type="file" accept=".xlsx,.xls" @change="importRoster" />
          <el-button type="primary" plain @click="openAssign">分配教师</el-button>
        </div>
        <el-table
          :data="detail.teacherAssignments || []"
          row-key="assignmentId"
          size="small"
          class="assignment-table"
        >
          <el-table-column prop="teacherName" label="教师" min-width="130" />
          <el-table-column label="任职" min-width="120"
            ><template #default="{ row }">{{
              row.type === 'HOMEROOM' ? '班主任' : row.subjectName || '任课教师'
            }}</template></el-table-column
          >
          <el-table-column prop="startsOn" label="开始日期" width="120" />
          <el-table-column label="状态" width="95"
            ><template #default="{ row }"
              ><el-tag :type="row.endsOn ? 'info' : 'success'">{{
                row.endsOn ? '已结束' : '任职中'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="操作" width="90"
            ><template #default="{ row }"
              ><el-button
                v-if="!row.endsOn"
                link
                type="danger"
                @click="removeAssignment(row.assignmentId)"
                >撤销</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <h2 class="roster-title">学生名册</h2>
        <el-table :data="detail.roster?.records || []" row-key="id" size="small">
          <el-table-column prop="studentNo" label="学号" min-width="120" />
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column prop="gender" label="性别" width="70" />
          <el-table-column prop="status" label="学籍状态" width="110">
            <template #default="{ row }">{{ statusLabel(row.status) }}</template>
          </el-table-column>
          <template #empty><el-empty description="班级暂无学生" /></template>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog
      v-model="editVisible"
      :title="editing?.id ? '编辑班级' : '新建班级'"
      width="480px"
      destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="校区" required>
          <el-select v-model="form.campusId" :disabled="!!editing" class="full-width">
            <el-option
              v-for="item in campuses"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学年" required>
          <el-select v-model="form.academicYearId" :disabled="!!editing" class="full-width">
            <el-option v-for="item in years" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <div class="form-pair">
          <el-form-item label="年级" required>
            <el-select v-model="form.grade" :disabled="!!editing" class="full-width">
              <el-option
                v-for="grade in 6"
                :key="grade"
                :label="`小学${gradeLabel(grade)}年级`"
                :value="grade"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="班级名称" required
            ><el-input v-model="form.className" placeholder="一班"
          /></el-form-item>
        </div>
        <el-form-item label="人数上限"
          ><el-input-number v-model="form.capacity" :min="1" :max="100"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配教师" width="480px" destroy-on-close>
      <el-form :model="assign" label-position="top">
        <el-form-item label="任职类型">
          <el-radio-group v-model="assign.type">
            <el-radio-button value="HOMEROOM">班主任</el-radio-button>
            <el-radio-button value="SUBJECT">任课教师</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="教师" required>
          <el-select
            v-model="assign.userId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchTeachers"
            :loading="teacherLoading"
            placeholder="搜索姓名或工号"
            class="full-width"
          >
            <el-option
              v-for="teacher in teachers"
              :key="teacher.userId"
              :label="teacherLabel(teacher)"
              :value="teacher.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="assign.type === 'SUBJECT'" label="任教学科" required>
          <el-select v-model="assign.subjectId" class="full-width">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任职开始日期"
          ><el-date-picker
            v-model="assign.startsOn"
            type="date"
            value-format="YYYY-MM-DD"
            class="full-width"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssignment">保存任职</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import * as XLSX from 'xlsx'
  import { SchoolOption, SchoolTeacher, studentApi } from '@/api/students'

  const years = ref<SchoolOption[]>([])
  const router = useRouter()
  const campuses = ref<SchoolOption[]>([])
  const classes = ref<Record<string, any>[]>([])
  const teachers = ref<SchoolTeacher[]>([])
  const subjects = ref<SchoolOption[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const teacherLoading = ref(false)
  const editVisible = ref(false)
  const detailVisible = ref(false)
  const assignVisible = ref(false)
  const editing = ref<Record<string, any> | null>(null)
  const detail = ref<Record<string, any> | null>(null)
  const rosterInput = ref<HTMLInputElement>()
  const filters = reactive<Record<string, any>>({ academicYearId: '', campusId: '', grade: '' })
  const form = reactive<Record<string, any>>({
    campusId: '',
    academicYearId: '',
    grade: 1,
    className: '',
    capacity: 40
  })
  const assign = reactive<Record<string, any>>({
    classId: '',
    userId: '',
    subjectId: '',
    type: 'HOMEROOM',
    startsOn: new Date().toISOString().slice(0, 10)
  })
  const filteredClasses = computed(() =>
    filters.grade
      ? classes.value.filter((item) => Number(item.grade) === Number(filters.grade))
      : classes.value
  )
  const gradeLabel = (grade: number) => ['', '一', '二', '三', '四', '五', '六'][grade] || grade
  const statusLabel = (status?: string) =>
    ({
      ACTIVE: '在用',
      INACTIVE: '停用',
      ARCHIVED: '已归档',
      ENROLLED: '在读',
      SUSPENDED: '休学',
      GRADUATED: '毕业',
      TRANSFERRED: '转出'
    })[status || ''] || '待核定'
  const teacherLabel = (teacher: SchoolTeacher) =>
    `${teacher.realName || teacher.nickname || teacher.username}${teacher.employeeNo ? ` · ${teacher.employeeNo}` : ''}`

  async function loadClasses() {
    loading.value = true
    try {
      classes.value = await studentApi.classes({
        academicYearId: filters.academicYearId || undefined,
        campusId: filters.campusId || undefined
      })
    } finally {
      loading.value = false
    }
  }
  async function loadOptions() {
    ;[years.value, campuses.value] = await Promise.all([studentApi.years(), studentApi.campuses()])
    filters.academicYearId =
      years.value.find((item) => item.current)?.id || years.value[0]?.id || ''
    await loadClasses()
  }
  function openCreate() {
    editing.value = null
    Object.assign(form, {
      campusId: campuses.value[0]?.id || '',
      academicYearId: filters.academicYearId || years.value[0]?.id || '',
      grade: 1,
      className: '',
      capacity: 40
    })
    editVisible.value = true
  }
  function openEdit(row: Record<string, any>) {
    editing.value = row
    Object.assign(form, {
      campusId: row.campusId,
      academicYearId: row.academicYearId,
      grade: row.grade,
      className: row.className,
      capacity: row.capacity || 40
    })
    editVisible.value = true
  }
  function openAssign() {
    if (!detail.value) return
    assign.classId = detail.value.id
    assign.userId = ''
    assign.subjectId = ''
    assign.type = 'HOMEROOM'
    assignVisible.value = true
    searchTeachers('')
  }
  async function saveClass() {
    if (!form.className?.trim() || (!editing.value && (!form.campusId || !form.academicYearId)))
      return ElMessage.warning('请补全班级信息')
    saving.value = true
    try {
      if (editing.value)
        await studentApi.updateClass(editing.value.id, {
          className: form.className.trim(),
          capacity: form.capacity
        })
      else await studentApi.createClass(form)
      editVisible.value = false
      ElMessage.success(editing.value ? '班级已更新' : '班级已创建')
      await loadClasses()
    } finally {
      saving.value = false
    }
  }
  async function setStatus(row: Record<string, any>, status: string) {
    try {
      await ElMessageBox.confirm(
        `${status === 'ARCHIVED' ? '归档后将保留历史名册。' : `确认将班级设为${statusLabel(status)}？`}`,
        '确认操作',
        { type: 'warning' }
      )
      await studentApi.classStatus(row.id, status)
      ElMessage.success('班级状态已更新')
      await loadClasses()
    } catch {
      /* cancelled */
    }
  }
  async function openDetail(row: Record<string, any>) {
    detailVisible.value = true
    detail.value = await studentApi.classDetail(row.id)
    subjects.value = await studentApi.subjects(Number(row.grade))
  }
  async function searchTeachers(search: string) {
    teacherLoading.value = true
    try {
      teachers.value = await studentApi.teachers(search)
    } finally {
      teacherLoading.value = false
    }
  }
  async function saveAssignment() {
    if (!assign.userId || (assign.type === 'SUBJECT' && !assign.subjectId))
      return ElMessage.warning('请选教师及任职信息')
    saving.value = true
    try {
      await studentApi.assignClassTeacher(assign.classId, {
        userId: assign.userId,
        subjectId: assign.type === 'SUBJECT' ? assign.subjectId : null,
        type: assign.type,
        startsOn: assign.startsOn
      })
      assignVisible.value = false
      ElMessage.success('任职关系已保存')
      await loadClasses()
      if (detail.value) detail.value = await studentApi.classDetail(detail.value.id)
    } finally {
      saving.value = false
    }
  }
  async function removeAssignment(assignmentId: string) {
    if (!detail.value) return
    try {
      await ElMessageBox.confirm('结束该教师在本班的任职关系？', '确认撤销', { type: 'warning' })
      await studentApi.removeClassTeacher(detail.value.id, assignmentId)
      detail.value = await studentApi.classDetail(detail.value.id)
      await loadClasses()
      ElMessage.success('教师任职已结束')
    } catch {
      /* cancelled */
    }
  }
  async function importRoster(event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file || !detail.value) return
    try {
      const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' })
      const rows = XLSX.utils
        .sheet_to_json<Record<string, unknown>>(workbook.Sheets[workbook.SheetNames[0]], {
          defval: ''
        })
        .map((row) => ({
          studentNo: String(row['学号'] || row.studentNo || '').trim(),
          name: String(row['姓名'] || row.name || '').trim(),
          gender: String(row['性别'] || row.gender || ''),
          birthday: String(row['出生日期'] || row.birthday || '')
        }))
        .filter((row) => row.studentNo && row.name)
      const result = await studentApi.importClassRoster(detail.value.id, rows)
      ElMessage.success(`导入 ${result.imported}/${result.total} 人`)
      if (result.errors.length)
        await ElMessageBox.alert(
          result.errors
            .slice(0, 12)
            .map((item) => `第${item.row}行 ${item.studentNo}: ${item.message}`)
            .join('\n'),
          '部分数据未导入'
        )
      detail.value = await studentApi.classDetail(detail.value.id)
      await loadClasses()
    } finally {
      input.value = ''
    }
  }
  onMounted(loadOptions)
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
    margin-bottom: 22px;
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
  .heading-actions {
    display: flex;
    gap: 8px;
  }
  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;
    border-top: 1px solid var(--el-border-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  .filter-row .el-select {
    width: 190px;
  }
  .result-count {
    margin-left: auto;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .table-shell {
    padding-top: 8px;
  }
  .class-link {
    display: grid;
    gap: 4px;
    padding: 0;
    border: 0;
    background: transparent;
    color: inherit;
    text-align: left;
    cursor: pointer;
  }
  .class-link strong {
    color: var(--el-color-primary);
    font-size: 14px;
    font-weight: 600;
  }
  .class-link span,
  small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
  .detail-summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
    padding: 4px 0 22px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  .detail-summary div {
    display: grid;
    gap: 5px;
    min-width: 0;
  }
  .detail-summary span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
  .detail-summary strong {
    overflow-wrap: anywhere;
    font-size: 14px;
    font-weight: 600;
  }
  .roster-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 18px 0 10px;
  }
  .roster-toolbar h2 {
    flex: 1;
    margin: 0;
    font-size: 16px;
  }
  .form-pair {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
  .assignment-table {
    margin-bottom: 18px;
  }
  .roster-title {
    margin: 8px 0 10px;
    font-size: 16px;
  }
  .full-width {
    width: 100%;
  }
  @media (max-width: 640px) {
    .school-page {
      padding: 16px;
    }
    .page-heading {
      align-items: start;
      flex-direction: column;
    }
    .filter-row .el-select {
      width: 100%;
    }
    .result-count {
      width: 100%;
      margin: 0;
    }
    .detail-summary {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .roster-toolbar {
      align-items: start;
      flex-wrap: wrap;
    }
    .roster-toolbar h2 {
      flex-basis: 100%;
    }
  }
</style>

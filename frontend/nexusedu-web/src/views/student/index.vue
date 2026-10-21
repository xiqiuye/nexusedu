<template>
  <main class="student-page">
    <header class="page-head">
      <div
        ><span class="eyebrow">STUDENT ROSTER</span><h1>学生档案</h1
        ><p>按学年、校区和班级查看学生，学籍调整保留历史。</p></div
      >
      <el-button @click="settingsOpen = true">学校组织设置</el-button>
      <div class="actions"
        ><el-button @click="downloadTemplate">导入模板</el-button
        ><el-button @click="fileInput?.click()">批量导入</el-button
        ><input
          ref="fileInput"
          type="file"
          accept=".xlsx,.xls"
          hidden
          @change="importFile"
        /><el-button @click="exportRows">导出当前页</el-button
        ><el-button type="primary" @click="openCreate">新增学生</el-button></div
      >
    </header>
    <section class="filters">
      <el-input
        v-model="filters.search"
        clearable
        placeholder="搜索姓名或学号"
        class="search"
        @keyup.enter="reload(true)"
      />
      <el-select
        v-model="filters.academicYearId"
        clearable
        placeholder="全部学年"
        @change="reload(true)"
        ><el-option v-for="x in years" :key="x.id" :label="x.name" :value="x.id"
      /></el-select>
      <el-select v-model="filters.campusId" clearable placeholder="全部校区" @change="loadClasses"
        ><el-option v-for="x in campuses" :key="x.id" :label="x.name" :value="x.id"
      /></el-select>
      <el-select v-model="filters.grade" clearable placeholder="全部年级" @change="reload(true)"
        ><el-option v-for="n in 6" :key="n" :label="`小学${gradeLabel(n)}`" :value="n"
      /></el-select>
      <el-select v-model="filters.classId" clearable placeholder="全部班级" @change="reload(true)"
        ><el-option v-for="x in classes" :key="x.id" :label="x.className" :value="x.id"
      /></el-select>
      <el-select v-model="filters.status" clearable placeholder="全部状态" @change="reload(true)"
        ><el-option label="在读" value="ENROLLED" /><el-option
          label="休学"
          value="SUSPENDED" /><el-option label="转出" value="TRANSFERRED" /><el-option
          label="毕业"
          value="GRADUATED" /><el-option label="待核定" value="NEEDS_REVIEW"
      /></el-select>
      <el-button type="primary" plain @click="reload(true)">查询</el-button>
    </section>
    <section class="table-shell">
      <div class="table-meta"
        ><strong>学生名册</strong><span>共 {{ total }} 位学生</span></div
      >
      <el-table :data="rows" v-loading="loading" row-key="id" stripe>
        <el-table-column prop="studentNo" label="学号" min-width="130" /><el-table-column
          prop="name"
          label="姓名"
          min-width="120"
        /><el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="campusName" label="校区" min-width="120" /><el-table-column
          label="年级班级"
          min-width="140"
          ><template #default="s">{{
            s.row.grade
              ? `小学${gradeLabel(s.row.grade)} · ${s.row.className || '未分班'}`
              : '未分配'
          }}</template></el-table-column
        >
        <el-table-column prop="academicYear" label="学年" min-width="120" /><el-table-column
          label="学籍状态"
          width="100"
          ><template #default="s"
            ><el-tag :type="s.row.status === 'ENROLLED' ? 'success' : 'info'">{{
              statusLabel(s.row.status)
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="操作" width="100" fixed="right"
          ><template #default="s"
            ><el-button link type="primary" @click="openMove(s.row)">调整班级</el-button></template
          ></el-table-column
        >
        <template #empty><el-empty description="暂无符合条件的学生" /></template>
      </el-table>
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="reload()"
        @size-change="reload(true)"
      />
    </section>
    <el-dialog v-model="settingsOpen" title="学校组织设置" width="620px" destroy-on-close>
      <el-tabs>
        <el-tab-pane label="校区"
          ><el-form label-position="top"
            ><div class="form-grid"
              ><el-form-item label="校区编号"
                ><el-input v-model="campusForm.campusCode" /></el-form-item
              ><el-form-item label="校区名称"><el-input v-model="campusForm.name" /></el-form-item
              ><el-form-item label="地址" class="wide"
                ><el-input v-model="campusForm.address" /></el-form-item></div></el-form
          ><el-button type="primary" :loading="saving" @click="saveCampus"
            >创建校区</el-button
          ></el-tab-pane
        >
        <el-tab-pane label="学年"
          ><el-form label-position="top"
            ><div class="form-grid"
              ><el-form-item label="学年名称"
                ><el-input v-model="yearForm.name" placeholder="2026-2027" /></el-form-item
              ><el-form-item label="当前学年"><el-switch v-model="yearForm.current" /></el-form-item
              ><el-form-item label="开始日期"
                ><el-date-picker
                  v-model="yearForm.startsOn"
                  type="date"
                  value-format="YYYY-MM-DD" /></el-form-item
              ><el-form-item label="结束日期"
                ><el-date-picker
                  v-model="yearForm.endsOn"
                  type="date"
                  value-format="YYYY-MM-DD" /></el-form-item></div></el-form
          ><el-button type="primary" :loading="saving" @click="saveYear"
            >创建学年</el-button
          ></el-tab-pane
        >
        <el-tab-pane label="班级"
          ><el-form label-position="top"
            ><div class="form-grid"
              ><el-form-item label="校区"
                ><el-select v-model="classForm.campusId"
                  ><el-option
                    v-for="x in campuses"
                    :key="x.id"
                    :label="x.name"
                    :value="x.id" /></el-select></el-form-item
              ><el-form-item label="学年"
                ><el-select v-model="classForm.academicYearId"
                  ><el-option
                    v-for="x in years"
                    :key="x.id"
                    :label="x.name"
                    :value="x.id" /></el-select></el-form-item
              ><el-form-item label="年级"
                ><el-select v-model="classForm.grade"
                  ><el-option
                    v-for="n in 6"
                    :key="n"
                    :label="`小学${gradeLabel(n)}`"
                    :value="n" /></el-select></el-form-item
              ><el-form-item label="班级名称"
                ><el-input v-model="classForm.className" placeholder="一班" /></el-form-item
              ><el-form-item label="人数上限"
                ><el-input-number
                  v-model="classForm.capacity"
                  :min="1"
                  :max="100" /></el-form-item></div></el-form
          ><el-button type="primary" :loading="saving" @click="saveClass"
            >创建班级</el-button
          ></el-tab-pane
        >
        <el-tab-pane label="班主任"
          ><el-form label-position="top"
            ><div class="form-grid"
              ><el-form-item label="班级"
                ><el-select v-model="homeroomForm.classId"
                  ><el-option
                    v-for="x in allClasses"
                    :key="x.id"
                    :label="`${x.campusName} · ${x.academicYear} · 小学${gradeLabel(x.grade)}${x.className}`"
                    :value="x.id" /></el-select></el-form-item
              ><el-form-item label="教师账号 ID"
                ><el-input v-model="homeroomForm.userId" /></el-form-item
              ><el-form-item label="任职开始日期"
                ><el-date-picker
                  v-model="homeroomForm.startsOn"
                  type="date"
                  value-format="YYYY-MM-DD" /></el-form-item></div></el-form
          ><el-button type="primary" :loading="saving" @click="saveHomeroom"
            >保存任职</el-button
          ></el-tab-pane
        >
        <el-tab-pane label="校区管理员 / 任课教师">
          <el-form label-position="top">
            <div class="form-grid">
              <el-form-item label="校区">
                <el-select v-model="staffForm.campusId">
                  <el-option v-for="x in campuses" :key="x.id" :label="x.name" :value="x.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="班级（任课教师）">
                <el-select v-model="staffForm.classId">
                  <el-option
                    v-for="x in allClasses"
                    :key="x.id"
                    :label="`${x.campusName} · ${x.academicYear} · 小学${gradeLabel(x.grade)}${x.className}`"
                    :value="x.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="校内教师账号 ID" class="wide">
                <el-input v-model="staffForm.userId" />
              </el-form-item>
            </div>
          </el-form>
          <el-button :loading="saving" @click="saveCampusAdmin">设置校区管理员</el-button>
          <el-button type="primary" :loading="saving" @click="saveTeacher">设置任课班级</el-button>
        </el-tab-pane>
        >
      </el-tabs>
    </el-dialog>
    <el-dialog v-model="createOpen" title="新建学生档案" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"
        ><div class="form-grid">
          <el-form-item label="学号" prop="studentNo"
            ><el-input v-model="form.studentNo" /></el-form-item
          ><el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="性别"
            ><el-select v-model="form.gender" clearable
              ><el-option label="女" value="女" /><el-option
                label="男"
                value="男" /></el-select></el-form-item
          ><el-form-item label="出生日期"
            ><el-date-picker v-model="form.birthday" value-format="YYYY-MM-DD" type="date"
          /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item
          ><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="校区" prop="campusId"
            ><el-select v-model="form.campusId" @change="loadFormClasses"
              ><el-option
                v-for="x in campuses"
                :key="x.id"
                :label="x.name"
                :value="x.id" /></el-select
          ></el-form-item>
          <el-form-item label="学年" prop="academicYearId"
            ><el-select v-model="form.academicYearId" @change="loadFormClasses"
              ><el-option v-for="x in years" :key="x.id" :label="x.name" :value="x.id" /></el-select
          ></el-form-item>
          <el-form-item label="年级" prop="grade"
            ><el-select v-model="form.grade" @change="loadFormClasses"
              ><el-option
                v-for="n in 6"
                :key="n"
                :label="`小学${gradeLabel(n)}`"
                :value="n" /></el-select
          ></el-form-item>
          <el-form-item label="班级"
            ><el-select v-model="form.classId" clearable
              ><el-option
                v-for="x in formClasses"
                :key="x.id"
                :label="x.className"
                :value="x.id" /></el-select
          ></el-form-item> </div
      ></el-form>
      <template #footer
        ><el-button @click="createOpen = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="createStudent"
          >保存档案</el-button
        ></template
      >
    </el-dialog>
    <el-dialog v-model="moveOpen" title="调整校区与班级" width="520px" destroy-on-close>
      <p class="move-student">{{ selected?.studentNo }} · {{ selected?.name }}</p
      ><el-form label-position="top"
        ><div class="form-grid">
          <el-form-item label="校区"
            ><el-select v-model="moveForm.campusId" @change="loadMoveClasses"
              ><el-option
                v-for="x in campuses"
                :key="x.id"
                :label="x.name"
                :value="x.id" /></el-select
          ></el-form-item>
          <el-form-item label="年级"
            ><el-select v-model="moveForm.grade" @change="loadMoveClasses"
              ><el-option
                v-for="n in 6"
                :key="n"
                :label="`小学${gradeLabel(n)}`"
                :value="n" /></el-select
          ></el-form-item>
          <el-form-item label="目标班级"
            ><el-select v-model="moveForm.classId" clearable
              ><el-option
                v-for="x in moveClasses"
                :key="x.id"
                :label="x.className"
                :value="x.id" /></el-select
          ></el-form-item>
          <el-form-item label="生效日期"
            ><el-date-picker v-model="moveForm.effectiveOn" value-format="YYYY-MM-DD" type="date"
          /></el-form-item>
          <el-form-item label="调整原因" class="wide"
            ><el-input v-model="moveForm.reason"
          /></el-form-item> </div
      ></el-form>
      <template #footer
        ><el-button @click="moveOpen = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="moveStudent"
          >确认调整</el-button
        ></template
      >
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
  import * as XLSX from 'xlsx'
  import { studentApi, type SchoolOption, type StudentRow } from '@/api/students'
  const rows = ref<StudentRow[]>([]),
    campuses = ref<SchoolOption[]>([]),
    years = ref<SchoolOption[]>([])
  const classes = ref<SchoolOption[]>([]),
    formClasses = ref<SchoolOption[]>([]),
    moveClasses = ref<SchoolOption[]>([]),
    allClasses = ref<SchoolOption[]>([])
  const filters = reactive<Record<string, any>>({
    search: '',
    academicYearId: '',
    campusId: '',
    grade: '',
    classId: '',
    status: ''
  })
  const current = ref(1),
    size = ref(20),
    total = ref(0),
    loading = ref(false),
    saving = ref(false)
  const createOpen = ref(false),
    moveOpen = ref(false),
    settingsOpen = ref(false),
    formRef = ref<FormInstance>(),
    selected = ref<StudentRow>(),
    fileInput = ref<HTMLInputElement>()
  const form = reactive<Record<string, any>>({
    studentNo: '',
    name: '',
    gender: '',
    birthday: '',
    email: '',
    phone: '',
    campusId: '',
    academicYearId: '',
    grade: 1,
    classId: ''
  })
  const moveForm = reactive<Record<string, any>>({
    campusId: '',
    grade: 1,
    classId: '',
    effectiveOn: new Date().toISOString().slice(0, 10),
    reason: ''
  })
  const campusForm = reactive({ campusCode: '', name: '', address: '' })
  const yearForm = reactive({ name: '', startsOn: '', endsOn: '', current: true })
  const classForm = reactive<Record<string, any>>({
    campusId: '',
    academicYearId: '',
    grade: 1,
    className: '',
    capacity: 40
  })
  const homeroomForm = reactive({
    classId: '',
    userId: '',
    startsOn: new Date().toISOString().slice(0, 10)
  })
  const staffForm = reactive({ campusId: '', classId: '', userId: '' })
  const rules: FormRules = {
    studentNo: [{ required: true, message: '请填写学号', trigger: 'blur' }],
    name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
    campusId: [{ required: true, message: '请选择校区', trigger: 'change' }],
    academicYearId: [{ required: true, message: '请选择学年', trigger: 'change' }],
    grade: [{ required: true, message: '请选择年级', trigger: 'change' }]
  }
  const gradeLabel = (n: number) => ['', '一', '二', '三', '四', '五', '六'][n]
  const statusLabel = (v?: string) =>
    ({
      ENROLLED: '在读',
      SUSPENDED: '休学',
      TRANSFERRED: '转出',
      GRADUATED: '毕业',
      NEEDS_REVIEW: '待核定'
    })[v || ''] || '未分配'
  async function reload(reset = false) {
    if (reset) current.value = 1
    loading.value = true
    try {
      const p = await studentApi.list({ ...filters, current: current.value, size: size.value })
      rows.value = p.records || []
      total.value = p.total || 0
    } catch {
      rows.value = []
    } finally {
      loading.value = false
    }
  }
  async function loadClasses() {
    filters.classId = ''
    classes.value = await studentApi.classes({
      campusId: filters.campusId || undefined,
      academicYearId: filters.academicYearId || undefined
    })
    reload(true)
  }
  async function loadFormClasses() {
    form.classId = ''
    formClasses.value = await studentApi.classes({
      campusId: form.campusId,
      academicYearId: form.academicYearId
    })
  }
  async function loadMoveClasses() {
    moveForm.classId = ''
    moveClasses.value = await studentApi.classes({
      campusId: moveForm.campusId,
      academicYearId: selected.value?.academicYearId
    })
  }
  async function refreshSchoolOptions() {
    ;[campuses.value, years.value] = await Promise.all([studentApi.campuses(), studentApi.years()])
    allClasses.value = await studentApi.classes({})
  }
  async function saveCampus() {
    saving.value = true
    try {
      await studentApi.createCampus(campusForm)
      await refreshSchoolOptions()
      Object.assign(campusForm, { campusCode: '', name: '', address: '' })
      ElMessage.success('校区已创建')
    } finally {
      saving.value = false
    }
  }
  async function saveYear() {
    saving.value = true
    try {
      await studentApi.createYear(yearForm)
      await refreshSchoolOptions()
      ElMessage.success('学年已创建')
    } finally {
      saving.value = false
    }
  }
  async function saveClass() {
    saving.value = true
    try {
      await studentApi.createClass(classForm)
      await refreshSchoolOptions()
      ElMessage.success('班级已创建')
    } finally {
      saving.value = false
    }
  }
  async function saveHomeroom() {
    saving.value = true
    try {
      await studentApi.assignHomeroom(
        homeroomForm.classId,
        homeroomForm.userId,
        homeroomForm.startsOn
      )
      await refreshSchoolOptions()
      ElMessage.success('班主任任职已保存')
    } finally {
      saving.value = false
    }
  }
  async function saveCampusAdmin() {
    if (!staffForm.campusId || !staffForm.userId)
      return ElMessage.warning('请选择校区并填写账号 ID')
    saving.value = true
    try {
      await studentApi.assignCampusAdmin(staffForm.campusId, staffForm.userId)
      ElMessage.success('校区管理员已设置')
    } finally {
      saving.value = false
    }
  }
  async function saveTeacher() {
    if (!staffForm.classId || !staffForm.userId) return ElMessage.warning('请选择班级并填写账号 ID')
    saving.value = true
    try {
      await studentApi.assignTeacher(staffForm.classId, staffForm.userId)
      ElMessage.success('任课班级已设置')
    } finally {
      saving.value = false
    }
  }
  function openCreate() {
    Object.assign(form, {
      studentNo: '',
      name: '',
      gender: '',
      birthday: '',
      email: '',
      phone: '',
      campusId: campuses.value[0]?.id || '',
      academicYearId: years.value.find((y) => y.current)?.id || years.value[0]?.id || '',
      grade: 1,
      classId: ''
    })
    createOpen.value = true
    loadFormClasses()
  }
  async function createStudent() {
    await formRef.value?.validate()
    saving.value = true
    try {
      await studentApi.create(form)
      ElMessage.success('学生档案已创建')
      createOpen.value = false
      reload()
    } finally {
      saving.value = false
    }
  }
  function openMove(row: StudentRow) {
    selected.value = row
    Object.assign(moveForm, {
      campusId: row.campusId || campuses.value[0]?.id,
      grade: row.grade || 1,
      classId: row.classId || '',
      effectiveOn: new Date().toISOString().slice(0, 10),
      reason: ''
    })
    moveOpen.value = true
    loadMoveClasses()
  }
  async function moveStudent() {
    if (!selected.value?.enrollmentId) {
      ElMessage.warning('该学生没有有效学年记录')
      return
    }
    saving.value = true
    try {
      await studentApi.move(selected.value.id, {
        ...moveForm,
        enrollmentId: selected.value.enrollmentId
      })
      ElMessage.success('学籍安排已调整')
      moveOpen.value = false
      reload()
    } finally {
      saving.value = false
    }
  }
  function exportRows() {
    const ws = XLSX.utils.json_to_sheet(
      rows.value.map((x) => ({
        学号: x.studentNo,
        姓名: x.name,
        性别: x.gender,
        校区: x.campusName,
        学年: x.academicYear,
        年级: x.grade ? `小学${gradeLabel(x.grade)}年级` : '',
        班级: x.className,
        状态: statusLabel(x.status)
      }))
    )
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '学生名册')
    XLSX.writeFile(wb, '学生名册.xlsx')
  }
  function downloadTemplate() {
    const ws = XLSX.utils.aoa_to_sheet([
      ['学号', '姓名', '性别', '出生日期', '邮箱', '联系电话', '校区', '学年', '年级', '班级'],
      ['填写学号', '', '', '', '', '', '校区名称', '2026-2027', '1', '一年级一班']
    ])
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '导入模板')
    XLSX.writeFile(wb, '小学学生导入模板.xlsx')
  }
  async function importFile(event: Event) {
    const input = event.target as HTMLInputElement,
      file = input.files?.[0]
    if (!file) return
    try {
      const book = XLSX.read(await file.arrayBuffer(), { type: 'array' })
      const raw = XLSX.utils.sheet_to_json<Record<string, unknown>>(
        book.Sheets[book.SheetNames[0]],
        { defval: '' }
      )
      const values = raw
        .map((x) => ({
          studentNo: String(x['学号'] || '').trim(),
          name: String(x['姓名'] || '').trim(),
          gender: String(x['性别'] || '').trim(),
          birthday: String(x['出生日期'] || '').trim(),
          email: String(x['邮箱'] || '').trim(),
          phone: String(x['联系电话'] || '').trim(),
          campusName: String(x['校区'] || '').trim(),
          academicYear: String(x['学年'] || '').trim(),
          gradeNo: Number(x['年级']) || undefined,
          className: String(x['班级'] || '').trim()
        }))
        .filter((x) => x.studentNo || x.name)
      if (!values.length) {
        ElMessage.warning('表格中没有学生数据')
        return
      }
      const result = await studentApi.import(values)
      await reload(true)
      if (result.errors?.length) {
        const html = result.errors
          .map((x) => `第${x.row}行 ${x.studentNo}：${x.message}`)
          .join('<br>')
        await ElMessageBox.alert(html, `成功导入 ${result.imported}/${result.total} 条`, {
          dangerouslyUseHTMLString: true
        })
      } else ElMessage.success(`成功导入 ${result.imported} 位学生`)
    } catch {
      ElMessage.error('导入失败，请检查模板和组织名称')
    } finally {
      input.value = ''
    }
  }
  onMounted(async () => {
    ;[campuses.value, years.value] = await Promise.all([studentApi.campuses(), studentApi.years()])
    await reload()
  })
</script>

<style scoped lang="scss">
  .student-page {
    max-width: 1500px;
    margin: auto;
    padding: 28px;
    color: var(--el-text-color-primary);
  }
  .page-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 20px;
    margin-bottom: 24px;
  }
  .eyebrow {
    color: #748279;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.16em;
  }
  h1 {
    margin: 7px 0 5px;
    color: #25352d;
    font-size: 28px;
    font-weight: 650;
  }
  p {
    margin: 0;
    color: #78847d;
  }
  .actions {
    display: flex;
    flex-wrap: wrap;
    gap: 9px;
  }
  .filters {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 18px;
    padding: 16px;
    border: 1px solid #e7ece7;
    border-radius: 8px;
    background: #f6f8f5;
  }
  .filters :deep(.el-select) {
    width: 150px;
  }
  .search {
    width: 220px;
  }
  .table-shell {
    padding: 0 18px 14px;
    border: 1px solid #e7ece7;
    border-radius: 8px;
    background: white;
  }
  .table-meta {
    display: flex;
    height: 54px;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
  }
  .table-meta span {
    color: #7c8981;
    font-size: 12px;
  }
  .el-pagination {
    justify-content: flex-end;
    margin-top: 16px;
  }
  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 16px;
  }
  .form-grid :deep(.el-select),
  .form-grid :deep(.el-date-editor) {
    width: 100%;
  }
  .move-student {
    margin-bottom: 18px;
    padding: 12px;
    border-radius: 6px;
    background: #f5f7f4;
    color: #314239;
  }
  .wide {
    grid-column: 1 / -1;
  }
  @media (max-width: 760px) {
    .student-page {
      padding: 16px;
    }
    .page-head {
      align-items: flex-start;
      flex-direction: column;
    }
    .filters {
      display: grid;
      grid-template-columns: 1fr 1fr;
    }
    .search,
    .filters :deep(.el-select) {
      width: 100%;
    }
    .form-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

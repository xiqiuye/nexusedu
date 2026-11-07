<template>
  <main class="school-page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">TEACHING ORGANIZATION</span>
        <h1>班主任 / 任课关系</h1>
      </div>
      <el-button type="primary" @click="loadClasses">刷新列表</el-button>
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
    </section>

    <section class="table-shell">
      <el-table v-loading="loading" :data="classes" row-key="id" stripe>
        <el-table-column label="班级" min-width="190">
          <template #default="{ row }">
            <strong>小学{{ gradeLabel(row.grade) }}年级{{ row.className }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="academicYear" label="学年" min-width="130" />
        <el-table-column prop="campusName" label="校区" min-width="130" />
        <el-table-column prop="homeroomTeacher" label="班主任" min-width="150">
          <template #default="{ row }">{{ row.homeroomTeacher || '未安排' }}</template>
        </el-table-column>
        <el-table-column prop="teachers" label="任课教师" min-width="200">
          <template #default="{ row }">{{ row.teachers || '未安排' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssign(row)">分配关系</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无班级，请先在班级管理中创建班级" /></template>
      </el-table>
    </section>

    <el-dialog v-model="dialog" title="分配班主任 / 任课教师" width="480px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="班级"><el-input :model-value="classLabel" disabled /></el-form-item>
        <el-form-item label="关系类型" required>
          <el-radio-group v-model="form.type" @change="loadSubjects">
            <el-radio-button label="HOMEROOM">班主任</el-radio-button>
            <el-radio-button label="SUBJECT">任课教师</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="教师" required>
          <el-select
            v-model="form.userId"
            filterable
            remote
            :remote-method="searchTeachers"
            :loading="teacherLoading"
            class="full-width"
            placeholder="搜索教师"
          >
            <el-option
              v-for="teacher in teachers"
              :key="teacher.userId"
              :label="teacherLabel(teacher)"
              :value="teacher.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'SUBJECT'" label="任教学科" required>
          <el-select v-model="form.subjectId" class="full-width">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="form.startsOn"
            type="date"
            value-format="YYYY-MM-DD"
            class="full-width"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssignment">保存关系</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { studentApi, type SchoolOption, type SchoolTeacher } from '@/api/students'

  const years = ref<SchoolOption[]>([])
  const campuses = ref<SchoolOption[]>([])
  const classes = ref<Record<string, any>[]>([])
  const teachers = ref<SchoolTeacher[]>([])
  const subjects = ref<SchoolOption[]>([])
  const loading = ref(false)
  const teacherLoading = ref(false)
  const saving = ref(false)
  const dialog = ref(false)
  const selectedClass = ref<Record<string, any>>()
  const filters = reactive<Record<string, any>>({ academicYearId: '', campusId: '', grade: '' })
  const form = reactive({
    classId: '',
    userId: '',
    subjectId: '',
    type: 'HOMEROOM',
    startsOn: new Date().toISOString().slice(0, 10)
  })
  const classLabel = computed(() =>
    selectedClass.value
      ? `小学${gradeLabel(selectedClass.value.grade)}年级${selectedClass.value.className}`
      : ''
  )
  const gradeLabel = (grade: number) => ['', '一', '二', '三', '四', '五', '六'][grade] || grade
  const teacherLabel = (teacher: SchoolTeacher) =>
    `${teacher.realName || teacher.nickname || teacher.username}${teacher.employeeNo ? ` · ${teacher.employeeNo}` : ''}`

  async function loadClasses() {
    loading.value = true
    try {
      const rows = await studentApi.classes({
        academicYearId: filters.academicYearId || undefined,
        campusId: filters.campusId || undefined
      })
      classes.value = filters.grade
        ? rows.filter((row) => Number(row.grade) === Number(filters.grade))
        : rows
    } finally {
      loading.value = false
    }
  }
  async function searchTeachers(search: string) {
    teacherLoading.value = true
    try {
      teachers.value = await studentApi.teachers(search)
    } finally {
      teacherLoading.value = false
    }
  }
  async function loadSubjects() {
    if (form.type === 'SUBJECT' && selectedClass.value) {
      subjects.value = await studentApi.subjects(Number(selectedClass.value.grade))
    }
  }
  async function openAssign(row: Record<string, any>) {
    selectedClass.value = row
    Object.assign(form, {
      classId: row.id,
      userId: '',
      subjectId: '',
      type: 'HOMEROOM',
      startsOn: new Date().toISOString().slice(0, 10)
    })
    dialog.value = true
    await searchTeachers('')
  }
  async function saveAssignment() {
    if (!form.userId || (form.type === 'SUBJECT' && !form.subjectId)) {
      ElMessage.warning('请选择教师及任职信息')
      return
    }
    saving.value = true
    try {
      await studentApi.assignClassTeacher(form.classId, {
        userId: form.userId,
        subjectId: form.type === 'SUBJECT' ? form.subjectId : null,
        type: form.type,
        startsOn: form.startsOn
      })
      dialog.value = false
      ElMessage.success('任职关系已保存')
      await loadClasses()
    } finally {
      saving.value = false
    }
  }
  onMounted(async () => {
    ;[years.value, campuses.value] = await Promise.all([studentApi.years(), studentApi.campuses()])
    filters.academicYearId = years.value.find((item) => item.current)?.id || ''
    await loadClasses()
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
    margin-bottom: 24px;
  }
  .page-heading h1 {
    margin: 6px 0 0;
    font-size: 26px;
  }
  .eyebrow {
    color: var(--el-color-primary);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.12em;
  }
  .filter-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 18px;
  }
  .filter-row :deep(.el-select) {
    width: 190px;
  }
  .table-shell {
    overflow: hidden;
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
    background: var(--el-bg-color);
  }
  .full-width {
    width: 100%;
  }
</style>

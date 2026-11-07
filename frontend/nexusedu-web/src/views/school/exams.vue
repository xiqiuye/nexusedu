<template>
  <main class="school-page">
    <header class="page-heading">
      <div><span class="eyebrow">ASSESSMENT</span><h1>考试管理</h1></div>
      <el-button type="primary" @click="dialog = true">新建考试</el-button>
    </header>
    <section class="filter-row">
      <el-select v-model="filters.academicYearId" clearable placeholder="全部学年" @change="load">
        <el-option v-for="item in years" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.campusId" clearable placeholder="全部校区" @change="load">
        <el-option v-for="item in campuses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </section>
    <section class="table-shell">
      <el-table v-loading="loading" :data="exams" row-key="id" stripe>
        <el-table-column prop="name" label="考试名称" min-width="180" />
        <el-table-column prop="academicYear" label="学年" min-width="130" />
        <el-table-column prop="campusName" label="校区" min-width="130" />
        <el-table-column label="年级" width="100"
          ><template #default="{ row }"
            >小学{{ gradeLabel(row.grade) }}年级</template
          ></el-table-column
        >
        <el-table-column prop="term" label="学期" width="120" />
        <el-table-column prop="examDate" label="考试日期" width="130" />
        <template #empty><el-empty description="暂无考试记录" /></template>
      </el-table>
    </section>
    <el-dialog v-model="dialog" title="新建考试" width="500px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="考试名称" required
          ><el-input v-model="form.name" placeholder="例如：期中检测"
        /></el-form-item>
        <el-form-item label="学年" required
          ><el-select v-model="form.academicYearId" class="full-width"
            ><el-option
              v-for="item in years"
              :key="item.id"
              :label="item.name"
              :value="item.id" /></el-select
        ></el-form-item>
        <el-form-item label="校区" required
          ><el-select v-model="form.campusId" class="full-width"
            ><el-option
              v-for="item in campuses"
              :key="item.id"
              :label="item.name"
              :value="item.id" /></el-select
        ></el-form-item>
        <el-form-item label="年级" required
          ><el-select v-model="form.grade" class="full-width"
            ><el-option
              v-for="grade in 6"
              :key="grade"
              :label="`小学${gradeLabel(grade)}年级`"
              :value="grade" /></el-select
        ></el-form-item>
        <el-form-item label="学期" required
          ><el-select v-model="form.term" class="full-width"
            ><el-option label="第一学期" value="FIRST" /><el-option
              label="第二学期"
              value="SECOND" /><el-option label="暑期" value="SUMMER" /></el-select
        ></el-form-item>
        <el-form-item label="考试日期" required
          ><el-date-picker
            v-model="form.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            class="full-width"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">创建考试</el-button></template
      >
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { studentApi, type SchoolOption } from '@/api/students'

  const years = ref<SchoolOption[]>([])
  const campuses = ref<SchoolOption[]>([])
  const exams = ref<Record<string, any>[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const dialog = ref(false)
  const filters = reactive<Record<string, any>>({ academicYearId: '', campusId: '' })
  const form = reactive<Record<string, any>>({
    name: '',
    academicYearId: '',
    campusId: '',
    grade: 1,
    term: 'FIRST',
    examDate: new Date().toISOString().slice(0, 10)
  })
  const gradeLabel = (grade: number) => ['', '一', '二', '三', '四', '五', '六'][grade] || grade

  async function load() {
    loading.value = true
    try {
      exams.value = await studentApi.exams(filters)
    } finally {
      loading.value = false
    }
  }
  async function save() {
    if (!form.name.trim() || !form.academicYearId || !form.campusId)
      return ElMessage.warning('请填写考试信息')
    saving.value = true
    try {
      await studentApi.createExam(form)
      dialog.value = false
      ElMessage.success('考试已创建')
      await load()
    } finally {
      saving.value = false
    }
  }
  onMounted(async () => {
    ;[years.value, campuses.value] = await Promise.all([studentApi.years(), studentApi.campuses()])
    form.academicYearId = years.value.find((item) => item.current)?.id || years.value[0]?.id || ''
    form.campusId = campuses.value[0]?.id || ''
    await load()
  })
</script>

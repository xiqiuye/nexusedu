<template>
  <main class="school-page">
    <header class="page-heading">
      <div><span class="eyebrow">ASSESSMENT</span><h1>考试成绩</h1></div>
    </header>
    <section class="filter-row">
      <el-select v-model="filters.academicYearId" placeholder="学年" @change="loadExams">
        <el-option v-for="item in years" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.campusId" placeholder="校区" @change="loadExams">
        <el-option v-for="item in campuses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.grade" placeholder="年级" @change="loadExams">
        <el-option
          v-for="grade in 6"
          :key="grade"
          :label="`小学${gradeLabel(grade)}年级`"
          :value="grade"
        />
      </el-select>
      <el-select
        v-model="selectedExamId"
        placeholder="选择考试"
        class="exam-select"
        @change="loadRosterOptions"
      >
        <el-option
          v-for="exam in exams"
          :key="exam.id"
          :label="`${exam.name} · ${exam.academicYear}`"
          :value="exam.id"
        />
      </el-select>
    </section>
    <section v-if="selectedExam" class="score-toolbar">
      <div class="exam-summary"
        ><strong>{{ selectedExam.name }}</strong
        ><span
          >{{ selectedExam.academicYear }} · 小学{{ gradeLabel(selectedExam.grade) }}年级 ·
          {{ selectedExam.examDate }}</span
        ></div
      >
      <el-select v-model="classId" placeholder="选择班级" @change="loadRoster">
        <el-option
          v-for="item in classes"
          :key="item.id"
          :label="item.className"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="subjectId" placeholder="选择科目" @change="loadRoster">
        <el-option v-for="item in subjects" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" :disabled="!roster.length" :loading="saving" @click="saveScores"
        >保存成绩</el-button
      >
    </section>
    <section v-if="selectedExam && classId && subjectId" class="table-shell">
      <el-table v-loading="loadingRoster" :data="roster" row-key="id" stripe>
        <el-table-column prop="studentNo" label="学号" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="140" />
        <el-table-column label="成绩" width="180">
          <template #default="{ row }"
            ><el-input-number
              v-model="row.score"
              :min="0"
              :max="100"
              :precision="2"
              :disabled="row.absent"
              controls-position="right"
          /></template>
        </el-table-column>
        <el-table-column label="缺考" width="100"
          ><template #default="{ row }"
            ><el-checkbox v-model="row.absent" @change="row.score = null" /></template
        ></el-table-column>
        <el-table-column label="备注" min-width="200"
          ><template #default="{ row }"
            ><el-input v-model="row.remark" placeholder="可选" /></template
        ></el-table-column>
        <template #empty><el-empty description="该班级暂无在读学生" /></template>
      </el-table>
    </section>
    <el-empty v-else description="选择考试、班级和科目后录入成绩" />
  </main>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { SchoolOption, studentApi } from '@/api/students'
  const years = ref<SchoolOption[]>([])
  const campuses = ref<SchoolOption[]>([])
  const exams = ref<Record<string, any>[]>([])
  const classes = ref<Record<string, any>[]>([])
  const subjects = ref<SchoolOption[]>([])
  const roster = ref<Record<string, any>[]>([])
  const selectedExamId = ref('')
  const classId = ref('')
  const subjectId = ref('')
  const loadingRoster = ref(false)
  const saving = ref(false)
  const filters = reactive<Record<string, any>>({ academicYearId: '', campusId: '', grade: 1 })
  const selectedExam = computed(() => exams.value.find((item) => item.id === selectedExamId.value))
  const gradeLabel = (grade?: number) => ['', '一', '二', '三', '四', '五', '六'][grade || 0] || grade || ''
  async function loadExams() {
    classId.value = ''
    subjectId.value = ''
    roster.value = []
    exams.value = await studentApi.exams(filters)
    selectedExamId.value = exams.value[0]?.id || ''
    if (selectedExam.value) await loadRosterOptions()
  }
  async function loadRosterOptions() {
    if (!selectedExam.value) return
    const exam = selectedExam.value
    filters.academicYearId = exam.academicYearId
    filters.campusId = exam.campusId
    filters.grade = exam.grade
    ;[classes.value, subjects.value] = await Promise.all([
      studentApi.classes({ academicYearId: exam.academicYearId, campusId: exam.campusId }),
      studentApi.subjects(Number(exam.grade))
    ])
    classes.value = classes.value.filter((item) => Number(item.grade) === Number(exam.grade))
    classId.value = classes.value[0]?.id || ''
    subjectId.value = subjects.value[0]?.id || ''
    await loadRoster()
  }
  async function loadRoster() {
    if (!selectedExam.value || !classId.value || !subjectId.value) {
      roster.value = []
      return
    }
    loadingRoster.value = true
    try {
      const [page, scores] = await Promise.all([
        studentApi.list({
          academicYearId: selectedExam.value.academicYearId,
          campusId: selectedExam.value.campusId,
          grade: selectedExam.value.grade,
          classId: classId.value,
          status: 'ENROLLED',
          current: 1,
          size: 200
        }),
        studentApi.scores(selectedExamId.value, classId.value)
      ])
      const scoreByStudent = new Map(
        scores
          .filter((item) => String(item.subjectId) === String(subjectId.value))
          .map((item) => [String(item.studentId), item])
      )
      roster.value = page.records.map((student) => {
        const existing = scoreByStudent.get(String(student.id))
        return {
          ...student,
          score: existing?.score ?? null,
          absent: !!existing?.absent,
          remark: existing?.remark || ''
        }
      })
    } finally {
      loadingRoster.value = false
    }
  }
  async function saveScores() {
    if (roster.value.some((row) => !row.absent && row.score == null))
      return ElMessage.warning('请填写所有学生成绩或标记缺考')
    saving.value = true
    try {
      for (const row of roster.value) {
        await studentApi.saveScore({
          examId: selectedExamId.value,
          classId: classId.value,
          studentId: row.id,
          subjectId: subjectId.value,
          score: row.score,
          absent: row.absent,
          remark: row.remark
        })
      }
      ElMessage.success(`已保存 ${roster.value.length} 条成绩`)
      await loadRoster()
    } finally {
      saving.value = false
    }
  }
  onMounted(async () => {
    ;[years.value, campuses.value] = await Promise.all([studentApi.years(), studentApi.campuses()])
    filters.academicYearId =
      years.value.find((item) => item.current)?.id || years.value[0]?.id || ''
    filters.campusId = campuses.value[0]?.id || ''
    await loadExams()
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
  .filter-row,
  .score-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;
    border-block: 1px solid var(--el-border-color-lighter);
  }
  .filter-row .el-select {
    width: 180px;
  }
  .exam-select {
    min-width: 230px;
  }
  .score-toolbar {
    justify-content: end;
    border-top: 0;
  }
  .exam-summary {
    display: grid;
    gap: 4px;
    margin-right: auto;
  }
  .exam-summary span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
  .score-toolbar .el-select {
    width: 180px;
  }
  .table-shell {
    padding-top: 10px;
  }
  .form-pair {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
  .full-width {
    width: 100%;
  }
  @media (max-width: 700px) {
    .school-page {
      padding: 16px;
    }
    .page-heading {
      align-items: start;
      flex-direction: column;
    }
    .filter-row .el-select,
    .score-toolbar .el-select {
      width: 100%;
    }
    .exam-summary {
      flex-basis: 100%;
    }
  }
</style>

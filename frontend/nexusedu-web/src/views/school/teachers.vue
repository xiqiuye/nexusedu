<template>
  <main class="school-page">
    <header class="page-heading">
      <div><span class="eyebrow">STAFF DIRECTORY</span><h1>教师管理</h1></div>
      <el-button type="primary" @click="openTeacher()">登记教师档案</el-button>
    </header>
    <el-tabs v-model="tab">
      <el-tab-pane label="教师档案" name="teachers">
        <div class="filter-row">
          <el-input
            v-model="search"
            clearable
            placeholder="搜索姓名、账号或工号"
            @keyup.enter="loadTeachers"
          />
          <el-button @click="loadTeachers">查询</el-button>
          <span class="result-count">{{ profiles.length }} 位教师</span>
        </div>
        <el-table v-loading="loading" :data="profiles" row-key="userId" stripe>
          <el-table-column prop="employeeNo" label="工号" min-width="130"
            ><template #default="{ row }">{{
              row.employeeNo || '待登记'
            }}</template></el-table-column
          >
          <el-table-column label="姓名" min-width="150"
            ><template #default="{ row }">{{
              row.realName || row.nickname || row.username
            }}</template></el-table-column
          >
          <el-table-column prop="username" label="登录账号" min-width="140" />
          <el-table-column prop="title" label="职务/职称" min-width="140"
            ><template #default="{ row }">{{ row.title || '—' }}</template></el-table-column
          >
          <el-table-column label="档案状态" width="110"
            ><template #default="{ row }"
              ><el-tag :type="row.teacherId ? 'success' : 'warning'">{{
                row.teacherId ? '已登记' : '未登记'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="操作" width="120"
            ><template #default="{ row }"
              ><el-button v-if="!row.teacherId" link type="primary" @click="openTeacher(row)"
                >登记档案</el-button
              ><el-button v-else link @click="ElMessage.info('任教关系可在班级管理中维护')"
                >查看任教</el-button
              ></template
            ></el-table-column
          >
          <template #empty><el-empty description="没有匹配的账号" /></template>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="科目设置" name="subjects">
        <div class="filter-row">
          <el-select v-model="subjectGrade" clearable placeholder="全部年级" @change="loadSubjects">
            <el-option
              v-for="grade in 6"
              :key="grade"
              :label="`小学${gradeLabel(grade)}年级`"
              :value="grade"
            />
          </el-select>
          <el-button type="primary" @click="subjectVisible = true">新增科目</el-button>
        </div>
        <el-table v-loading="subjectLoading" :data="subjects" row-key="id" stripe>
          <el-table-column prop="subjectCode" label="科目代码" min-width="150" />
          <el-table-column prop="name" label="科目名称" min-width="160" />
          <el-table-column label="适用年级" min-width="180"
            ><template #default="{ row }"
              >小学{{ gradeLabel(row.gradeMin) }}至{{ gradeLabel(row.gradeMax) }}年级</template
            ></el-table-column
          >
          <el-table-column label="状态" width="100"
            ><template #default><el-tag type="success">启用</el-tag></template></el-table-column
          >
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="teacherVisible" title="登记教师档案" width="480px" destroy-on-close>
      <el-form :model="teacherForm" label-position="top">
        <el-form-item label="系统账号" required>
          <el-select
            v-model="teacherForm.userId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userLoading"
            class="full-width"
            placeholder="搜索已启用的系统账号"
          >
            <el-option
              v-for="item in candidates"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
              :disabled="!!item.teacherId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工号" required
          ><el-input v-model="teacherForm.employeeNo"
        /></el-form-item>
        <el-form-item label="职务/职称"
          ><el-input v-model="teacherForm.title" placeholder="班主任、语文教师等"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="teacherVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveTeacher"
          >保存档案</el-button
        ></template
      >
    </el-dialog>

    <el-dialog v-model="subjectVisible" title="新增科目" width="440px" destroy-on-close>
      <el-form :model="subjectForm" label-position="top">
        <el-form-item label="科目代码" required
          ><el-input v-model="subjectForm.subjectCode" placeholder="例如 MUSIC"
        /></el-form-item>
        <el-form-item label="科目名称" required
          ><el-input v-model="subjectForm.name"
        /></el-form-item>
        <div class="form-pair">
          <el-form-item label="起始年级"
            ><el-select v-model="subjectForm.gradeMin"
              ><el-option
                v-for="grade in 6"
                :key="grade"
                :label="`小学${gradeLabel(grade)}年级`"
                :value="grade" /></el-select
          ></el-form-item>
          <el-form-item label="结束年级"
            ><el-select v-model="subjectForm.gradeMax"
              ><el-option
                v-for="grade in 6"
                :key="grade"
                :label="`小学${gradeLabel(grade)}年级`"
                :value="grade" /></el-select
          ></el-form-item>
        </div>
      </el-form>
      <template #footer
        ><el-button @click="subjectVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveSubject"
          >保存科目</el-button
        ></template
      >
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { SchoolOption, SchoolTeacher, studentApi } from '@/api/students'
  const tab = ref('teachers')
  const profiles = ref<SchoolTeacher[]>([])
  const candidates = ref<SchoolTeacher[]>([])
  const subjects = ref<SchoolOption[]>([])
  const search = ref('')
  const subjectGrade = ref<number>()
  const loading = ref(false)
  const subjectLoading = ref(false)
  const userLoading = ref(false)
  const saving = ref(false)
  const teacherVisible = ref(false)
  const subjectVisible = ref(false)
  const teacherForm = reactive({ userId: '', employeeNo: '', title: '' })
  const subjectForm = reactive({ subjectCode: '', name: '', gradeMin: 1, gradeMax: 6 })
  const gradeLabel = (grade: number) => ['', '一', '二', '三', '四', '五', '六'][grade] || grade
  const userLabel = (user: SchoolTeacher) =>
    `${user.realName || user.nickname || user.username} · ${user.username}`
  async function loadTeachers() {
    loading.value = true
    try {
      profiles.value = await studentApi.teachers(search.value)
    } finally {
      loading.value = false
    }
  }
  async function searchUsers(value: string) {
    userLoading.value = true
    try {
      candidates.value = await studentApi.teacherCandidates(value)
    } finally {
      userLoading.value = false
    }
  }
  async function loadSubjects() {
    subjectLoading.value = true
    try {
      subjects.value = await studentApi.subjects(subjectGrade.value)
    } finally {
      subjectLoading.value = false
    }
  }
  function openTeacher(user?: SchoolTeacher) {
    Object.assign(teacherForm, {
      userId: user?.userId || '',
      employeeNo: user?.employeeNo || '',
      title: user?.title || ''
    })
    teacherVisible.value = true
    if (user) candidates.value = [user]
    else searchUsers('')
  }
  async function saveTeacher() {
    if (!teacherForm.userId || !teacherForm.employeeNo.trim())
      return ElMessage.warning('请选择账号并填写工号')
    saving.value = true
    try {
      await studentApi.createTeacher(teacherForm)
      teacherVisible.value = false
      ElMessage.success('教师档案已登记')
      await loadTeachers()
    } finally {
      saving.value = false
    }
  }
  async function saveSubject() {
    if (!subjectForm.subjectCode.trim() || !subjectForm.name.trim())
      return ElMessage.warning('请填写科目代码和名称')
    saving.value = true
    try {
      await studentApi.createSubject(subjectForm)
      subjectVisible.value = false
      Object.assign(subjectForm, { subjectCode: '', name: '', gradeMin: 1, gradeMax: 6 })
      ElMessage.success('科目已创建')
      await loadSubjects()
    } finally {
      saving.value = false
    }
  }
  onMounted(() => {
    loadTeachers()
    loadSubjects()
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
  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;
  }
  .filter-row .el-input {
    width: min(360px, 100%);
  }
  .filter-row .el-select {
    width: 190px;
  }
  .result-count {
    margin-left: auto;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .full-width {
    width: 100%;
  }
  .form-pair {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
  @media (max-width: 640px) {
    .school-page {
      padding: 16px;
    }
    .page-heading {
      align-items: start;
      flex-direction: column;
    }
    .result-count {
      width: 100%;
      margin: 0;
    }
  }
</style>

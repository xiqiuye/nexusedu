<template>
  <main class="school-page">
    <header class="page-heading">
      <div
        ><span class="eyebrow">SCHOOL ORGANIZATION</span><h1>{{ title }}</h1></div
      >
      <el-button type="primary" :disabled="!kind" @click="openCreate"
        >新增{{ title.replace('管理', '') }}</el-button
      >
    </header>
    <section v-if="kind === 'campus'" class="filter-row" aria-label="校区查询">
      <el-input
        v-model="filters.name"
        placeholder="校区名称"
        aria-label="校区名称"
        clearable
        @keyup.enter="applyFilters"
        @clear="applyFilters"
      />
      <el-input
        v-model="filters.campusCode"
        placeholder="校区编码"
        aria-label="校区编码"
        clearable
        @keyup.enter="applyFilters"
        @clear="applyFilters"
      />
      <el-select
        v-model="filters.status"
        placeholder="全部状态"
        aria-label="校区状态"
        clearable
        @change="applyFilters"
      >
        <el-option label="在用" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="applyFilters">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <span class="result-count">{{ filteredRows.length }} 个校区</span>
    </section>
    <section class="table-shell">
      <el-table v-loading="loading" :data="filteredRows" row-key="id" stripe>
        <el-table-column
          prop="name"
          :label="title.replace('管理', '名称')"
          min-width="200"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <strong v-if="kind === 'campus'" class="campus-name">{{ row.name }}</strong>
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="kind === 'campus'"
          prop="campusCode"
          label="校区编码"
          min-width="140"
        />
        <el-table-column
          v-if="kind === 'campus'"
          prop="address"
          label="地址"
          min-width="240"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.address || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="kind === 'campus'" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
              {{ row.status === 'ACTIVE' ? '在用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="kind === 'campus'" label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              type="danger"
              :loading="deletingId === row.id"
              :disabled="!!deletingId"
              @click="removeCampus(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
        <el-table-column v-if="kind === 'year'" prop="startsOn" label="开始日期" min-width="140" />
        <el-table-column v-if="kind === 'year'" prop="endsOn" label="结束日期" min-width="140" />
        <el-table-column v-if="kind === 'subject'" label="适用年级" min-width="180">
          <template #default="{ row }"
            >{{ gradeLabel(row.gradeMin) }}至{{ gradeLabel(row.gradeMax) }}年级</template
          >
        </el-table-column>
        <template #empty
          ><el-empty
            :description="
              kind === 'campus' ? '当前查询条件下没有校区' : `暂无${title.replace('管理', '')}数据`
            "
        /></template>
      </el-table>
    </section>
    <el-dialog
      v-model="dialog"
      :title="`${editing ? '编辑' : '新增'}${title.replace('管理', '')}`"
      width="min(520px, 94vw)"
      destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item v-if="kind === 'campus'" label="校区编码" required
          ><el-input v-model.trim="form.campusCode" maxlength="32" placeholder="例如 MAIN"
        /></el-form-item>
        <el-form-item :label="title.replace('管理', '名称')" required
          ><el-input
            v-model.trim="form.name"
            :maxlength="kind === 'campus' ? 128 : kind === 'subject' ? 64 : 32"
            :placeholder="`请输入${title.replace('管理', '名称')}`"
        /></el-form-item>
        <el-form-item v-if="kind === 'campus'" label="地址"
          ><el-input
            v-model="form.address"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请输入校区地址"
        /></el-form-item>
        <template v-if="kind === 'year'">
          <el-form-item label="开始日期" required
            ><el-date-picker v-model="form.startsOn" value-format="YYYY-MM-DD" class="full-width"
          /></el-form-item>
          <el-form-item label="结束日期" required
            ><el-date-picker v-model="form.endsOn" value-format="YYYY-MM-DD" class="full-width"
          /></el-form-item>
          <el-checkbox v-model="form.current">设为当前学年</el-checkbox>
        </template>
        <template v-if="kind === 'subject'">
          <el-form-item label="科目代码" required
            ><el-input v-model.trim="form.subjectCode" maxlength="32" placeholder="例如 MATH"
          /></el-form-item>
          <div class="form-pair">
            <el-form-item label="起始年级"
              ><el-select v-model="form.gradeMin"
                ><el-option
                  v-for="grade in 6"
                  :key="grade"
                  :label="gradeLabel(grade)"
                  :value="grade" /></el-select
            ></el-form-item>
            <el-form-item label="结束年级"
              ><el-select v-model="form.gradeMax"
                ><el-option
                  v-for="grade in 6"
                  :key="grade"
                  :label="gradeLabel(grade)"
                  :value="grade" /></el-select
            ></el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存</el-button></template
      >
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref, watch } from 'vue'
  import { useRoute } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    studentApi,
    type AcademicYearCreateRequest,
    type CampusCreateRequest,
    type SchoolOption,
    type SubjectCreateRequest
  } from '@/api/students'

  type CatalogKind = 'campus' | 'year' | 'subject'
  const props = defineProps<{ kind?: CatalogKind }>()
  const route = useRoute()
  // Existing database menus can still load /school/catalog without a wrapper prop.
  const legacyKinds: Record<string, CatalogKind | undefined> = {
    CampusManagement: 'campus',
    AcademicYearManagement: 'year',
    SubjectManagement: 'subject'
  }
  const kind = computed(() => props.kind ?? legacyKinds[String(route.name)])
  const title = computed(() =>
    kind.value
      ? { campus: '校区管理', year: '学年管理', subject: '学科管理' }[kind.value]
      : '教学目录'
  )
  const rows = ref<SchoolOption[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const dialog = ref(false)
  const editing = ref<SchoolOption | null>(null)
  const deletingId = ref('')
  const filters = reactive({ name: '', campusCode: '', status: '' })
  const appliedFilters = reactive({ ...filters })
  const filteredRows = computed(() => {
    if (kind.value !== 'campus') return rows.value
    const name = appliedFilters.name.trim().toLocaleLowerCase()
    const code = appliedFilters.campusCode.trim().toLocaleLowerCase()
    return rows.value.filter(
      (row) =>
        row.name.toLocaleLowerCase().includes(name) &&
        (row.campusCode || '').toLocaleLowerCase().includes(code) &&
        (!appliedFilters.status || row.status === appliedFilters.status)
    )
  })
  const form = reactive<CampusCreateRequest & AcademicYearCreateRequest & SubjectCreateRequest>({
    campusCode: '',
    name: '',
    address: '',
    startsOn: '',
    endsOn: '',
    current: true,
    subjectCode: '',
    gradeMin: 1,
    gradeMax: 6
  })
  const gradeLabel = (grade: number) =>
    `小学${['', '一', '二', '三', '四', '五', '六'][grade] || grade}`

  function applyFilters() {
    Object.assign(appliedFilters, filters)
  }
  function resetFilters() {
    Object.assign(filters, { name: '', campusCode: '', status: '' })
    applyFilters()
  }
  function openCreate() {
    editing.value = null
    Object.assign(form, {
      campusCode: '',
      name: '',
      address: '',
      startsOn: '',
      endsOn: '',
      current: true,
      subjectCode: '',
      gradeMin: 1,
      gradeMax: 6
    })
    dialog.value = true
  }
  function openEdit(row: SchoolOption) {
    editing.value = row
    Object.assign(form, {
      campusCode: row.campusCode || '',
      name: row.name,
      address: row.address || ''
    })
    dialog.value = true
  }
  function showCampusError(error: unknown) {
    let problem: unknown =
      error && typeof error === 'object' && 'data' in error ? error.data : undefined
    if (typeof problem === 'string') {
      try {
        problem = JSON.parse(problem)
      } catch {
        /* Use the fallback message below. */
      }
    }
    const detail =
      problem && typeof problem === 'object' && 'detail' in problem ? problem.detail : undefined
    ElMessage.error(typeof detail === 'string' ? detail : '操作失败，请稍后重试')
  }
  async function removeCampus(row: SchoolOption) {
    if (deletingId.value) return
    try {
      await ElMessageBox.confirm(
        `确定删除校区“${row.name}”吗？有关联业务记录的校区不能删除。`,
        '删除校区',
        {
          type: 'warning',
          confirmButtonText: '删除',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return
    }
    deletingId.value = row.id
    try {
      await studentApi.deleteCampus(row.id)
      ElMessage.success('校区已删除')
      await load()
    } catch (error) {
      showCampusError(error)
    } finally {
      deletingId.value = ''
    }
  }

  async function load() {
    if (!kind.value) return
    loading.value = true
    try {
      rows.value =
        kind.value === 'campus'
          ? await studentApi.campuses()
          : kind.value === 'year'
            ? await studentApi.years()
            : await studentApi.subjects()
    } finally {
      loading.value = false
    }
  }
  watch(kind, () => {
    dialog.value = false
    editing.value = null
    resetFilters()
    load()
  })
  async function save() {
    if (!kind.value) return ElMessage.warning('页面类型无法识别，请重新进入管理菜单')
    if (!form.name.trim()) return ElMessage.warning('请填写名称')
    if (kind.value === 'campus') {
      if (!form.campusCode.trim()) return ElMessage.warning('请填写校区编码')
      if (form.campusCode.trim().length > 32) return ElMessage.warning('校区编码不能超过32个字符')
      if (form.name.trim().length > 128) return ElMessage.warning('校区名称不能超过128个字符')
      if ((form.address || '').trim().length > 255)
        return ElMessage.warning('校区地址不能超过255个字符')
    }
    if (kind.value === 'subject') {
      if (!form.subjectCode.trim()) return ElMessage.warning('请填写科目代码')
      if (form.subjectCode.trim().length > 32) return ElMessage.warning('科目代码不能超过32个字符')
      if (form.name.trim().length > 64) return ElMessage.warning('学科名称不能超过64个字符')
      if (
        !Number.isInteger(form.gradeMin) ||
        !Number.isInteger(form.gradeMax) ||
        form.gradeMin < 1 ||
        form.gradeMax > 6 ||
        form.gradeMax < form.gradeMin
      )
        return ElMessage.warning('请选择有效的年级范围，结束年级不能小于起始年级')
    }
    saving.value = true
    try {
      if (kind.value === 'campus') {
        const data: CampusCreateRequest = {
          campusCode: form.campusCode.trim(),
          name: form.name.trim(),
          address: (form.address || '').trim()
        }
        if (editing.value) await studentApi.updateCampus(editing.value.id, data)
        else await studentApi.createCampus(data, false)
      } else if (kind.value === 'year') await studentApi.createYear(form)
      else if (kind.value === 'subject')
        await studentApi.createSubject({
          subjectCode: form.subjectCode.trim(),
          name: form.name.trim(),
          gradeMin: form.gradeMin,
          gradeMax: form.gradeMax
        })
      dialog.value = false
      ElMessage.success(editing.value ? '校区已更新' : '保存成功')
      await load()
    } catch (error) {
      if (kind.value === 'campus') showCampusError(error)
      else throw error
    } finally {
      saving.value = false
    }
  }
  onMounted(load)
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
  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;
    border-top: 1px solid var(--el-border-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  .filter-row .el-input,
  .filter-row .el-select {
    width: 190px;
  }
  .filter-actions {
    display: flex;
  }
  .result-count {
    margin-left: auto;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .table-shell {
    padding-top: 8px;
  }
  .campus-name {
    color: var(--el-color-primary);
    font-size: 14px;
    font-weight: 600;
  }
  .form-pair {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
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
    .filter-row .el-input,
    .filter-row .el-select {
      width: 100%;
    }
    .result-count {
      width: 100%;
      margin: 0;
    }
  }
</style>

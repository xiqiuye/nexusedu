// Run the compiled SFC setup with HTTP calls stubbed, including legacy menu entries.
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import * as vue from 'vue'
import { compileScript, parse } from '@vue/compiler-sfc'

const file = new URL('../src/views/school/catalog.vue', import.meta.url)
const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
const compiled = compileScript(descriptor, { id: 'catalog-routing-regression' })
const code = ts.transpileModule(compiled.content, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText

function setup(props, name) {
  const requests = []
  const warnings = []
  const errors = []
  const confirmation = { cancelled: false }
  const route = vue.reactive({ name })
  const studentApi = Object.fromEntries(
    [
      'campuses',
      'years',
      'subjects',
      'createCampus',
      'createYear',
      'createSubject',
      'updateCampus',
      'deleteCampus'
    ].map((method) => [
      method,
      async (data, body) => {
        requests.push({ method, data, body })
        return []
      }
    ])
  )
  const context = {
    exports: {},
    require(specifier) {
      if (specifier === 'vue') return { ...vue, onMounted() {}, watch() {} }
      if (specifier === 'vue-router') return { useRoute: () => route }
      if (specifier === 'element-plus')
        return {
          ElMessage: {
            warning: (message) => warnings.push(message),
            error: (message) => errors.push(message),
            success() {}
          },
          ElMessageBox: {
            confirm: async () => {
              if (confirmation.cancelled) throw new Error('cancel')
            }
          }
        }
      if (specifier === '@/api/students') return { studentApi }
      throw new Error(`Unexpected import: ${specifier}`)
    }
  }
  vm.runInNewContext(code, context)
  const page = context.exports.default.setup(props, { expose() {} })
  Object.assign(page.form, {
    name: '测试目录',
    campusCode: 'MAIN',
    address: '测试地址',
    startsOn: '2026-09-01',
    endsOn: '2027-08-31',
    subjectCode: 'MATH'
  })
  return { page, requests, warnings, errors, confirmation, route }
}

for (const [name, kind, list, create, title] of [
  ['CampusManagement', 'campus', 'campuses', 'createCampus', '校区管理'],
  ['AcademicYearManagement', 'year', 'years', 'createYear', '学年管理'],
  ['SubjectManagement', 'subject', 'subjects', 'createSubject', '学科管理']
]) {
  // Old database menus load catalog.vue directly, without a kind prop.
  for (const props of [{}, { kind }]) {
    const { page, requests } = setup(props, name)
    assert.equal(page.title.value, title)
    await page.load()
    assert.equal(requests[0].method, list, `${name} list endpoint`)
    await page.save()
    assert.equal(requests[1].method, create, `${name} create endpoint`)
    assert.equal(requests[2].method, list, `${name} reload endpoint`)
  }
}

const unknown = setup({}, 'UnknownManagement')
await unknown.page.load()
await unknown.page.save()
assert.equal(unknown.requests.length, 0, 'an unknown page must not default to subjects')
assert.equal(unknown.warnings.length, 1)

const campus = setup({ kind: 'campus' }, 'CampusManagement')
campus.page.form.campusCode = '  '
await campus.page.save()
assert.equal(campus.requests.length, 0, 'missing campus code must be validated locally')

const managing = setup({ kind: 'campus' }, 'CampusManagement')
const existing = {
  id: '1',
  name: '阳光学校',
  campusCode: 'A01',
  address: '阳光路',
  status: 'ACTIVE'
}
managing.page.rows.value = [
  existing,
  { id: '2', name: '默认校区', campusCode: 'MAIN', status: 'ACTIVE' }
]
managing.page.filters.name = '阳光'
assert.equal(managing.page.filteredRows.value.length, 2, 'typing waits for the query action')
managing.page.applyFilters()
assert.equal(managing.page.filteredRows.value.length, 1)
managing.page.filters.campusCode = 'main'
managing.page.applyFilters()
assert.equal(managing.page.filteredRows.value.length, 0, 'combine query fields')
managing.page.resetFilters()
assert.equal(managing.page.filteredRows.value.length, 2)
managing.page.openEdit(existing)
assert.equal(managing.page.form.campusCode, 'A01')
managing.page.form.name = '阳光新校区'
await managing.page.save()
assert.equal(managing.requests[0].method, 'updateCampus')
assert.equal(managing.requests[0].data, '1')
assert.equal(managing.requests[0].body.name, '阳光新校区')
managing.page.openCreate()
assert.equal(managing.page.editing.value, null)
assert.equal(managing.page.form.campusCode, '')

managing.confirmation.cancelled = true
await managing.page.removeCampus(existing)
assert.equal(managing.requests.filter((item) => item.method === 'deleteCampus').length, 0)
managing.confirmation.cancelled = false
await managing.page.removeCampus(existing)
assert.equal(managing.requests.filter((item) => item.method === 'deleteCampus').length, 1)
assert.equal(managing.page.deletingId.value, '')
managing.page.showCampusError({ data: JSON.stringify({ detail: '校区存在关联记录，无法删除' }) })
assert.equal(managing.errors[0], '校区存在关联记录，无法删除')

console.log(
  'Catalog verified: legacy routes, queries, create/edit, delete confirmation, business errors.'
)

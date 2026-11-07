// Exercise the real catalog save handler without starting a browser or writing to MySQL.
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'

const file = new URL('../src/views/school/catalog.vue', import.meta.url)
const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
const source = ts.createSourceFile(
  file.pathname + '.ts',
  descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest,
  true
)
const handler = source.statements.find(
  (node) => ts.isFunctionDeclaration(node) && node.name.text === 'save'
)
assert.ok(handler, 'catalog save handler exists')
const code = ts.transpileModule(handler.getText(source), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

async function submit(overrides) {
  const requests = []
  const warnings = []
  const context = {
    form: { name: '数学', subjectCode: '', gradeMin: 1, gradeMax: 6, ...overrides },
    kind: { value: 'subject' },
    saving: { value: false },
    dialog: { value: true },
    editing: { value: null },
    ElMessage: { warning: (message) => warnings.push(message), success: () => {} },
    studentApi: { createSubject: async (data) => requests.push({ ...data }) },
    load: async () => {}
  }
  vm.createContext(context)
  vm.runInContext(code, context)
  await context.save()
  return { requests, warnings, context }
}

async function main() {
  for (const subjectCode of ['', '   ']) {
    const result = await submit({ subjectCode })
    assert.equal(result.requests.length, 0, 'missing subject code must not send a request')
    assert.equal(result.warnings.length, 1, 'show a local validation message')
    assert.equal(result.context.dialog.value, true, 'keep the form open')
  }
  for (const fields of [
    { name: '   ' },
    { subjectCode: 'X'.repeat(33) },
    { name: '数'.repeat(65) },
    { gradeMin: 6, gradeMax: 1 },
    { gradeMin: 0 },
    { gradeMax: 7 },
    { gradeMin: 1.5 }
  ]) {
    const result = await submit({ subjectCode: 'MATH', ...fields })
    assert.equal(result.requests.length, 0, 'reject invalid subject fields')
    assert.equal(result.warnings.length, 1)
  }
  const valid = await submit({ subjectCode: ' MATH ', name: ' 数学 ' })
  assert.equal(valid.requests.length, 1)
  assert.deepEqual(JSON.parse(JSON.stringify(valid.requests[0])), {
    subjectCode: 'MATH',
    name: '数学',
    gradeMin: 1,
    gradeMax: 6
  })
  assert.equal(valid.context.saving.value, false)
  console.log('Subject form verified: empty code, grade range, trimmed request payload.')
}
main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

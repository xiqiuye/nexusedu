import request from '@/utils/http'
export interface StudentRow {
  id: string
  enrollmentId?: string
  campusId?: string
  classId?: string
  academicYearId?: string
  studentNo: string
  name: string
  gender?: string
  birthday?: string
  campusName?: string
  academicYear?: string
  grade?: number
  className?: string
  status?: string
}
export interface StudentPage {
  records: StudentRow[]
  current: number
  size: number
  total: number
}
export interface SchoolOption {
  id: string
  name: string
  campusCode?: string
  address?: string
  startsOn?: string
  endsOn?: string
  current?: boolean
  subjectCode?: string
  gradeMin?: number
  gradeMax?: number
  campusId?: string
  academicYearId?: string
  campusName?: string
  academicYear?: string
  grade?: number
  className?: string
  capacity?: number
  status?: string
}
export interface CampusCreateRequest {
  campusCode: string
  name: string
  address?: string
}
export interface CampusRow extends CampusCreateRequest {
  id: string
  status: string
}
export interface AcademicYearCreateRequest {
  name: string
  startsOn: string
  endsOn: string
  current: boolean
}
export interface SubjectCreateRequest {
  subjectCode: string
  name: string
  gradeMin: number
  gradeMax: number
}
export interface ClassDetail extends SchoolOption {
  roster: StudentPage
  teacherAssignments: SchoolTeacher[]
}
export interface PromotionPreview {
  students: StudentRow[]
  targetClasses: SchoolOption[]
  defaultTargetGrade: number | null
  sourceGrade: number
  targetAcademicYearId: string
}
export interface PromotionBatch {
  batchId: string
  status: string
  items: Array<{ studentId: string; action: string; targetGrade?: number; reason?: string }>
}
export interface ExamRow {
  id: string
  name: string
  academicYearId: string
  examDate?: string
  status?: string
}
export interface ScoreRow {
  studentId: string
  studentNo: string
  name: string
  score?: number
  subjectId?: string
  absent?: boolean
  remark?: string
}
export interface SchoolTeacher {
  teacherId?: string
  userId: string
  username: string
  realName?: string
  nickname?: string
  employeeNo?: string
  title?: string
}
export const studentApi = {
  list: (params: Record<string, unknown>) =>
    request.get<StudentPage>({ url: '/api/school/students', params }),
  campuses: () => request.get<CampusRow[]>({ url: '/api/school/campuses' }),
  years: () => request.get<SchoolOption[]>({ url: '/api/school/academic-years' }),
  classes: (params: Record<string, unknown>) =>
    request.get<SchoolOption[]>({ url: '/api/school/classes', params }),
  classDetail: (id: string) => request.get<ClassDetail>({ url: `/api/school/classes/${id}` }),
  updateClass: (id: string, data: Record<string, unknown>) =>
    request.put<Record<string, any>>({ url: `/api/school/classes/${id}`, data }),
  classStatus: (id: string, status: string) =>
    request.put<void>({ url: `/api/school/classes/${id}/status`, data: { status } }),
  importClassRoster: (id: string, rows: Record<string, unknown>[]) =>
    request.post<{
      imported: number
      total: number
      errors: { row: number; studentNo: string; message: string }[]
    }>({
      url: `/api/school/classes/${id}/students/import`,
      data: rows
    }),
  teachers: (search: string) =>
    request.get<SchoolTeacher[]>({ url: '/api/school/teachers', params: { search } }),
  teacherCandidates: (search: string) =>
    request.get<SchoolTeacher[]>({ url: '/api/school/teacher-candidates', params: { search } }),
  createTeacher: (data: Record<string, unknown>) =>
    request.post<SchoolTeacher>({ url: '/api/school/teachers', data }),
  subjects: (grade?: number) =>
    request.get<SchoolOption[]>({ url: '/api/school/subjects', params: { grade } }),
  createSubject: (data: SubjectCreateRequest) =>
    request.post<SchoolOption>({ url: '/api/school/subjects', data }),
  assignClassTeacher: (classId: string, data: Record<string, unknown>) =>
    request.put<void>({ url: `/api/school/classes/${classId}/teachers`, data }),
  removeClassTeacher: (classId: string, assignmentId: string) =>
    request.del<void>({ url: `/api/school/classes/${classId}/teachers/${assignmentId}` }),
  previewPromotion: (data: Record<string, unknown>) =>
    request.post<PromotionPreview>({ url: '/api/school/promotions/preview', data }),
  confirmPromotion: (data: Record<string, unknown>) =>
    request.post<{ batchId: string; processed: number }>({
      url: '/api/school/promotions/confirm',
      data
    }),
  promotionBatch: (batchId: string) =>
    request.get<PromotionBatch>({ url: `/api/school/promotions/${batchId}` }),
  exams: (params: Record<string, unknown>) =>
    request.get<ExamRow[]>({ url: '/api/school/exams', params }),
  createExam: (data: Record<string, unknown>) =>
    request.post<ExamRow>({ url: '/api/school/exams', data }),
  scores: (examId: string, classId?: string) =>
    request.get<ScoreRow[]>({ url: '/api/school/scores', params: { examId, classId } }),
  saveScore: (data: Record<string, unknown>) =>
    request.post<void>({ url: '/api/school/scores', data }),
  create: (data: Record<string, unknown>) =>
    request.post<StudentRow>({ url: '/api/school/students', data }),
  import: (data: Record<string, unknown>[]) =>
    request.post<{
      imported: number
      total: number
      errors: { row: number; studentNo: string; message: string }[]
    }>({ url: '/api/school/students/import', data }),
  createCampus: (data: CampusCreateRequest, showErrorMessage = true) =>
    request.post<CampusRow>({ url: '/api/school/campuses', data, showErrorMessage }),
  updateCampus: (id: string, data: CampusCreateRequest) =>
    request.put<CampusRow>({ url: `/api/school/campuses/${id}`, data, showErrorMessage: false }),
  deleteCampus: (id: string) =>
    request.del<void>({ url: `/api/school/campuses/${id}`, showErrorMessage: false }),
  createYear: (data: AcademicYearCreateRequest) =>
    request.post<SchoolOption>({ url: '/api/school/academic-years', data }),
  createClass: (data: Record<string, unknown>) =>
    request.post<SchoolOption>({ url: '/api/school/classes', data }),
  assignHomeroom: (classId: string, userId: string, startsOn: string) =>
    request.put<void>({
      url: `/api/school/classes/${classId}/homeroom`,
      data: { userId, startsOn }
    }),
  assignTeacher: (classId: string, userId: string) =>
    request.put<void>({ url: `/api/school/classes/${classId}/teachers`, data: { userId } }),
  assignCampusAdmin: (campusId: string, userId: string) =>
    request.put<void>({ url: `/api/school/campuses/${campusId}/admins`, data: { userId } }),
  changeStatus: (id: string, data: Record<string, unknown>) =>
    request.put<void>({ url: `/api/school/students/${id}/status`, data }),
  move: (id: string, data: Record<string, unknown>) =>
    request.post<void>({ url: `/api/school/students/${id}/move`, data })
}

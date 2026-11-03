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
  [key: string]: any
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
  campuses: () => request.get<SchoolOption[]>({ url: '/api/school/campuses' }),
  years: () => request.get<SchoolOption[]>({ url: '/api/school/academic-years' }),
  classes: (params: Record<string, unknown>) =>
    request.get<SchoolOption[]>({ url: '/api/school/classes', params }),
  classDetail: (id: string) =>
    request.get<Record<string, any>>({ url: `/api/school/classes/${id}` }),
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
  createSubject: (data: Record<string, unknown>) =>
    request.post<SchoolOption>({ url: '/api/school/subjects', data }),
  assignClassTeacher: (classId: string, data: Record<string, unknown>) =>
    request.put<void>({ url: `/api/school/classes/${classId}/teachers`, data }),
  removeClassTeacher: (classId: string, assignmentId: string) =>
    request.del<void>({ url: `/api/school/classes/${classId}/teachers/${assignmentId}` }),
  previewPromotion: (data: Record<string, unknown>) =>
    request.post<Record<string, any>>({ url: '/api/school/promotions/preview', data }),
  confirmPromotion: (data: Record<string, unknown>) =>
    request.post<{ batchId: string; processed: number }>({
      url: '/api/school/promotions/confirm',
      data
    }),
  promotionBatch: (batchId: string) =>
    request.get<Record<string, any>>({ url: `/api/school/promotions/${batchId}` }),
  exams: (params: Record<string, unknown>) =>
    request.get<Record<string, any>[]>({ url: '/api/school/exams', params }),
  createExam: (data: Record<string, unknown>) =>
    request.post<Record<string, any>>({ url: '/api/school/exams', data }),
  scores: (examId: string, classId?: string) =>
    request.get<Record<string, any>[]>({ url: '/api/school/scores', params: { examId, classId } }),
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
  createCampus: (data: Record<string, unknown>) =>
    request.post<SchoolOption>({ url: '/api/school/campuses', data }),
  createYear: (data: Record<string, unknown>) =>
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

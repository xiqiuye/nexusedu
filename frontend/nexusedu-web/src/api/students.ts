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
export const studentApi = {
  list: (params: Record<string, unknown>) =>
    request.get<StudentPage>({ url: '/api/school/students', params }),
  campuses: () => request.get<SchoolOption[]>({ url: '/api/school/campuses' }),
  years: () => request.get<SchoolOption[]>({ url: '/api/school/academic-years' }),
  classes: (params: Record<string, unknown>) =>
    request.get<SchoolOption[]>({ url: '/api/school/classes', params }),
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

import type { AppRouteRecord } from '@/types/router'
export const studentRoutes: AppRouteRecord = {
  path: '/student',
  name: 'StudentManagement',
  component: '/index/index',
  meta: { title: '学生管理', icon: 'ri:graduation-cap-line' },
  children: [
    {
      path: 'roster',
      name: 'StudentRoster',
      component: '/student',
      meta: {
        title: '学生档案',
        icon: 'ri:contacts-line',
        authList: [
          { title: '学生档案', authMark: 'student:read' },
          { title: '维护学生档案', authMark: 'student:write' }
        ]
      }
    },
    {
      path: 'classes',
      name: 'ClassManagement',
      component: '/school/classes',
      meta: {
        title: '班级管理',
        icon: 'ri:group-line',
        authList: [{ title: '班级管理', authMark: 'student:read' }]
      }
    },
    {
      path: 'teachers',
      name: 'TeacherManagement',
      component: '/school/teachers',
      meta: {
        title: '教师管理',
        icon: 'ri:user-star-line',
        authList: [{ title: '教师管理', authMark: 'student:write' }]
      }
    },
    {
      path: 'promotion',
      name: 'AcademicPromotion',
      component: '/school/promotion',
      meta: {
        title: '学年升班',
        icon: 'ri:arrow-up-circle-line',
        authList: [{ title: '学年升班', authMark: 'student:write' }]
      }
    },
    {
      path: 'grades',
      name: 'GradeManagement',
      component: '/school/grades',
      meta: {
        title: '考试成绩',
        icon: 'ri:bar-chart-box-line',
        authList: [{ title: '考试成绩', authMark: 'student:write' }]
      }
    }
  ]
}

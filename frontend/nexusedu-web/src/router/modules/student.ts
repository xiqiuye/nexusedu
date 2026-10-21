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
    }
  ]
}

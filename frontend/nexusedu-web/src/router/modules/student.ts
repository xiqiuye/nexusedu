import type { AppRouteRecord } from '@/types/router'

export const studentRoutes: AppRouteRecord[] = [
  {
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
            { title: '查看学生', authMark: 'student:read' },
            { title: '维护学生', authMark: 'student:write' }
          ]
        }
      }
    ]
  },
  {
    path: '/organization',
    name: 'TeachingOrganization',
    component: '/index/index',
    meta: { title: '教学组织', icon: 'ri:building-2-line' },
    children: [
      {
        path: 'campuses',
        name: 'CampusManagement',
        component: '/school/campuses',
        meta: { title: '校区管理', icon: 'ri:map-pin-line' }
      },
      {
        path: 'academic-years',
        name: 'AcademicYearManagement',
        component: '/school/academic-years',
        meta: { title: '学年管理', icon: 'ri:calendar-2-line' }
      },
      {
        path: 'classes',
        name: 'ClassManagement',
        component: '/school/classes',
        meta: { title: '班级管理', icon: 'ri:group-line' }
      },
      {
        path: 'assignments',
        name: 'TeachingAssignments',
        component: '/school/assignments',
        meta: { title: '班主任/任课关系', icon: 'ri:user-settings-line' }
      },
      {
        path: 'promotion',
        name: 'AcademicPromotion',
        component: '/school/promotion',
        meta: { title: '学年升班', icon: 'ri:arrow-up-circle-line' }
      },
      {
        path: 'subjects',
        name: 'SubjectManagement',
        component: '/school/subjects',
        meta: { title: '学科管理', icon: 'ri:book-2-line' }
      }
    ]
  },
  {
    path: '/teachers',
    name: 'TeacherManagement',
    component: '/index/index',
    meta: { title: '教师管理', icon: 'ri:user-star-line' },
    children: [
      {
        path: 'profiles',
        name: 'TeacherProfiles',
        component: '/school/teachers',
        meta: { title: '教师档案', icon: 'ri:contacts-line' }
      }
    ]
  },
  {
    path: '/assessment',
    name: 'AssessmentManagement',
    component: '/index/index',
    meta: { title: '成绩管理', icon: 'ri:bar-chart-box-line' },
    children: [
      {
        path: 'exams',
        name: 'ExamManagement',
        component: '/school/exams',
        meta: { title: '考试管理', icon: 'ri:file-list-3-line' }
      },
      {
        path: 'scores',
        name: 'ScoreEntry',
        component: '/school/grades',
        meta: { title: '成绩录入', icon: 'ri:edit-box-line' }
      }
    ]
  }
]

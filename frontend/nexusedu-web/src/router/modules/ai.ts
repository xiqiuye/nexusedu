import { AppRouteRecord } from '@/types/router'
export const aiRoutes: AppRouteRecord = {
  path: '/ai',
  name: 'AiOperations',
  component: '/index/index',
  meta: { title: 'AI 运营', icon: 'ri:robot-line' },
  children: [
    {
      path: 'runs',
      name: 'AiRuns',
      component: '/ai/runs',
      meta: {
        title: '任务与步骤',
        icon: 'ri:flow-chart',
        authList: [{ title: '查询任务与步骤', authMark: 'ai:run:read' }]
      }
    },
    {
      path: 'logs',
      name: 'AiToolLogs',
      component: '/ai/logs',
      meta: {
        title: '工具执行日志',
        icon: 'ri:file-list-line',
        authList: [{ title: '查询工具日志', authMark: 'ai:log:read' }]
      }
    },
    {
      path: 'metrics',
      name: 'AiMetrics',
      component: '/ai/metrics',
      meta: {
        title: '指标与成本',
        icon: 'ri:bar-chart-box-line',
        authList: [
          { title: '查询指标与成本', authMark: 'ai:metrics:read' },
          { title: '配置模型价格', authMark: 'ai:price:create' }
        ]
      }
    },
    {
      path: 'evaluation',
      name: 'AiEvaluation',
      component: '/ai/evaluation',
      meta: {
        title: '离线评测',
        icon: 'ri:flask-line',
        authList: [
          { title: '查询评测', authMark: 'ai:evaluation:read' },
          { title: '维护评测集', authMark: 'ai:evaluation:write' },
          { title: '执行评测', authMark: 'ai:evaluation:run' }
        ]
      }
    },
    {
      path: 'configuration',
      name: 'AiConfiguration',
      component: '/ai/configuration',
      meta: {
        title: '配置与发布',
        icon: 'ri:settings-3-line',
        authList: [
          { title: '查询配置', authMark: 'ai:config:read' },
          { title: '编写版本', authMark: 'ai:config:write' },
          { title: '审批版本', authMark: 'ai:config:approve' },
          { title: '发布版本', authMark: 'ai:config:publish' },
          { title: '配置控制策略', authMark: 'ai:policy:write' }
        ]
      }
    }
  ]
}

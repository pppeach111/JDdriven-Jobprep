import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由分为两组：
 *
 * 1. 目标建立流程（带 goalId）：
 *      创建求职目标 -> 导入并解析 JD -> 查看能力与证据
 * 2. 一级导航的其余四个入口（尚无后端接口，页面内使用明确标识的演示数据）：
 *      岗位发现 / 测评与面试 / 学习计划 / 简历建议
 *
 * 名称约束：一级导航通过 `route.name` 判断选中项（见 App.vue 的 activeKey），
 * 因此这里的 name 必须与 App.vue 中 primaryNav 的 key 逐一对应。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/goals/new' },
    {
      path: '/goals/new',
      name: 'goal-create',
      component: () => import('@/views/GoalCreateView.vue'),
    },
    {
      path: '/goals/:goalId/jd',
      name: 'jd-import',
      component: () => import('@/views/JdImportView.vue'),
      props: true,
    },
    {
      path: '/goals/:goalId/capability',
      name: 'capability-map',
      component: () => import('@/views/CapabilityMapView.vue'),
      props: true,
    },
    {
      path: '/discovery',
      name: 'jobs',
      component: () => import('@/views/JobDiscoveryView.vue'),
    },
    {
      path: '/assessment',
      name: 'assessment',
      component: () => import('@/views/AssessmentView.vue'),
    },
    {
      path: '/learning',
      name: 'learning',
      component: () => import('@/views/LearningPlanView.vue'),
    },
    {
      path: '/resume',
      name: 'resume',
      component: () => import('@/views/ResumeAdviceView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/goals/new' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
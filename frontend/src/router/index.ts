import { createRouter, createWebHistory } from 'vue-router'

/**
 * 三页最小闭环：
 *   创建求职目标 -> 导入并解析 JD -> 查看能力图谱
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
    { path: '/:pathMatch(.*)*', redirect: '/goals/new' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
//路由配置
const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '数据看板',
          icon: 'PieChart'
        }
      },
      {
        path: 'knowledge',
        component: () => import('@/views/Knowledge.vue'),
        meta: {
          title: '健康数据',
          icon: 'DocumentAdd'
        }
      },
      {
        path: 'mental-health',
        component: () => import('@/views/MentalHealth.vue'),
        meta: {
          title: '心理健康测评',
          icon: 'Document'
        }
      },
      {
        path: 'schedule',
        component: () => import('@/views/Schedule.vue'),
        meta: {
          title: '制定计划',
          icon: 'EditPen'
        }
      },
      {
        path: 'consultation',
        component: () => import('@/views/Consulation.vue'),
        meta: {
          title: 'AI分析',
          icon: 'Service'
        }
      },
      {
        path: 'ai-chat',
        component: () => import('@/views/Chat.vue'),
        meta: {
          title: 'AI对话',
          icon: 'ChatDotRound'
        }
      },
      {
        path: 'content-recommend',
        component: () => import('@/views/ContentRecommend.vue'),
        meta: {
          title: '内容推荐',
          icon: 'VideoCamera'
        }
      },
      {
        path: 'physical-exam',
        component: () => import('@/views/PhysicalExam.vue'),
        meta: {
          title: '体检报告',
          icon: 'Files'
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: {
          title: '个人中心',
          icon: 'Avatar'
        }
      }
    ]
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        component: () => import('@/views/Login.vue'),
        meta: {
          title: '登录',

        }
      },
      {
        path: 'register',
        component: () => import('@/views/Register.vue'),
        meta: {
          title: '注册',

        }
      }
    ]
  }
]
const router = createRouter({
  history: createWebHistory(),
  routes: backendRoutes
})
export default router

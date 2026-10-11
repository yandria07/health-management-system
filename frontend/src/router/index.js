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
            path:'dashboard',
            component:()=>import('@/views/Dashboard.vue'),
            meta:{
                title: '数据看板',
                icon:'PieChart'
            }
        },
        {
            path: 'knowledge',
            component:()=>import('@/views/Knowledge.vue'),
            meta:{
                title: '健康数据',
                icon:'DocumentAdd'
            }
        },
        {
            path: 'schedule',
            component:()=>import('@/views/Schedule.vue'),
            meta:{
                title: '制定计划',
                icon:'EditPen'
            }
        },
        {
            path: 'consultation',
            component:()=>import('@/views/Consulation.vue'),  
            meta:{
                title: 'AI分析',
                icon:'Service'
            }
        }
    ]
    },
    {
        path: '/auth',
        component:AuthLayout,
        children: [
            {
                path:'login',
                component:()=>import('@/views/Login.vue'),
                meta:{
                    title: '登录',
                
                }
            },
            {
                path:'register',
                component:()=>import('@/views/Register.vue'),
                meta:{
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

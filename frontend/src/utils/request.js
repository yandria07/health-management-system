import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
//创建axios实例
const service = axios.create({
    baseURL: '/api',//请求前缀
    timeout: 5000,//超时时间
})
//创建请求拦截器
service.interceptors.request.use(
    config => {
        //在发送请求之前做些什么
        const token = localStorage.getItem('token')
        if(token){
            config.headers['token'] = token
        }
        return config
    },
    error=>{
        //对请求错误做些什么
        return Promise.reject(error)
    }
)
//创建响应拦截器
service.interceptors.response.use(
    response => {
         const { data, config } = response
    if (data.code === '200' || data.code === 200) {
      return data.data
    }

    // 登录过期
    if (data.code === '-1' || data.code === -1) {
      ElMessage.error(data.msg || data.message || '登录过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      if (!config.url.includes('/login')) {
        router.push('/auth/login')
      }
      return Promise.reject(new Error(data.msg || data.message || '登录过期'))
    }

    // 其他业务错误
    ElMessage.error(data.msg || data.message || '请求失败')
    return Promise.reject(new Error(data.msg || data.message || '请求失败'))
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
    }
)
export default service

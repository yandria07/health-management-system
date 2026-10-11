import request from '@/utils/request' // 你的 axios 实例

export const getPlanList = (data) => request.post('/health-plan/list', data)
export const addPlan = (data) => request.post('/health-plan/add', data)
export const updatePlan = (data) => request.post('/health-plan/update', data)
export const deletePlan = (id) => request.post(`/health-plan/del?id=${id}`)
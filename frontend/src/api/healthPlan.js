import request from '@/utils/request' // 你的 axios 实例

export const getPlanList = (data) => request.post('/health-plan/list', data)
export const addPlan = (data) => request.post('/health-plan/add', data)
export const updatePlan = (data) => request.post('/health-plan/update', data)
export const deletePlan = (id) => request.post(`/health-plan/del?id=${id}`)
// 打卡
export const checkinPlan = (planId) => request.post('/health-plan/checkin/do', { planId })
export const getCheckinOverview = (planIds) => request.post('/health-plan/checkin/overview', { planIds })
// 某计划的所有打卡日期（用于日历展示）
export const fetchCheckinDates = (planId) => request.post('/health-plan/checkin/dates', { planId })
// 发送测试提醒邮件（验证 SMTP 配置）
export const sendTestReminder = () => request.post('/health-plan/reminder/test')
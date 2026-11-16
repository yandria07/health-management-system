import request from '@/utils/request'

// 体检指标目录（分类 + 指标 + 参考范围）
export const getExamCatalog = () => request.post('/physical-exam/catalog')

// 报告列表 / 详情
export const getExamList = () => request.post('/physical-exam/list')
export const getExamDetail = (id) => request.post(`/physical-exam/detail?id=${id}`)

// 保存（新增或更新）/ 删除
export const saveExam = (data) => request.post('/physical-exam/save', data)
export const deleteExam = (id) => request.post(`/physical-exam/del?id=${id}`)

// AI 解读：大模型输出较慢，单独放宽超时到 90 秒
export const interpretExam = (id) => request.post(`/physical-exam/interpret?id=${id}`, {}, { timeout: 90000 })

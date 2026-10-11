import service from '@/utils/request'
export const register = (data) => {
    return service.post('/admin/add', data);

}
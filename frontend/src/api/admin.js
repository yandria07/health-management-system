import service from '@/utils/request'
export function login(data) {
    return service({
        url:'/admin/login',
        method:'post',
        data
    })   
}
export function categoryTree(data) {
    return service({
        url:'/knowledge/category/tree',
        method:'get',
    
    })
}

package com.health.healthserver.interceptor;

import com.health.healthserver.utils.JwtUtil;
import com.health.healthserver.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行浏览器的预检请求
        if ("OPTIONS".equals(request.getMethod())) return true;

        // 从请求头里拿 token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return reject(response, "未登录");
        }

        // 解析 userId
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            return reject(response, "登录过期，请重新登录");
        }

        // 存入 ThreadLocal，Controller 里就能直接用
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理，防止线程复用串数据
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"-1\",\"msg\":\"" + msg + "\"}");
        return false;
    }
}
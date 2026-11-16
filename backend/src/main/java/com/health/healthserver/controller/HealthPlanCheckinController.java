package com.health.healthserver.controller;

import com.health.healthserver.dto.CheckinStats;
import com.health.healthserver.response.R;
import com.health.healthserver.service.HealthPlanCheckinService;
import com.health.healthserver.utils.UserContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/health-plan/checkin")
public class HealthPlanCheckinController {

    @Resource
    private HealthPlanCheckinService checkinService;

    /** 打卡（幂等：同一天重复点击只记一次） */
    @PostMapping("/do")
    public R<?> doCheckin(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");
        Object pid = body.get("planId");
        if (pid == null) return R.fail("缺少 planId");
        Long planId = pid instanceof Number ? ((Number) pid).longValue()
                : Long.parseLong(pid.toString());
        checkinService.checkin(userId, planId);
        return R.success("打卡成功");
    }

    /** 批量概览：传入 planIds 列表，返回 planId -> 统计 */
    @PostMapping("/overview")
    public R<?> overview(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");
        Object idsObj = body.get("planIds");
        if (!(idsObj instanceof List)) return R.data(new java.util.HashMap<>());
        List<?> raw = (List<?>) idsObj;
        List<Long> planIds = raw.stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue()
                        : Long.parseLong(o.toString()))
                .collect(java.util.stream.Collectors.toList());
        Map<Long, CheckinStats> result = checkinService.overview(userId, planIds);
        return R.data(result);
    }

    /** 某计划的所有打卡日期，用于日历展示 */
    @PostMapping("/dates")
    public R<?> dates(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");
        Object pid = body.get("planId");
        if (pid == null) return R.fail("缺少 planId");
        Long planId = pid instanceof Number ? ((Number) pid).longValue()
                : Long.parseLong(pid.toString());
        return R.data(checkinService.dates(userId, planId));
    }
}

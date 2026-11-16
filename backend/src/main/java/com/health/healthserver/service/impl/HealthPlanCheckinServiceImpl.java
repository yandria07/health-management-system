package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthserver.dto.CheckinStats;
import com.health.healthserver.entity.HealthPlan;
import com.health.healthserver.entity.HealthPlanCheckin;
import com.health.healthserver.mapper.HealthPlanCheckinMapper;
import com.health.healthserver.service.HealthPlanCheckinService;
import com.health.healthserver.service.HealthPlanService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthPlanCheckinServiceImpl implements HealthPlanCheckinService {

    @Resource
    private HealthPlanCheckinMapper checkinMapper;
    @Resource
    private HealthPlanService healthPlanService;

    @Override
    public void checkin(Long userId, Long planId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<HealthPlanCheckin> existed = new LambdaQueryWrapper<>();
        existed.eq(HealthPlanCheckin::getUserId, userId)
                .eq(HealthPlanCheckin::getPlanId, planId)
                .eq(HealthPlanCheckin::getCheckinDate, today);
        if (checkinMapper.selectCount(existed) > 0) {
            return; // 今日已打卡，幂等
        }
        HealthPlanCheckin record = new HealthPlanCheckin();
        record.setUserId(userId);
        record.setPlanId(planId);
        record.setCheckinDate(today);
        checkinMapper.insert(record);
        // 打卡后判定是否达成目标
        evaluateAchievement(userId, planId);
    }

    @Override
    public CheckinStats stats(Long userId, Long planId) {
        LambdaQueryWrapper<HealthPlanCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthPlanCheckin::getUserId, userId)
                .eq(HealthPlanCheckin::getPlanId, planId)
                .select(HealthPlanCheckin::getCheckinDate);
        List<HealthPlanCheckin> list = checkinMapper.selectList(wrapper);

        Set<LocalDate> dates = list.stream()
                .map(HealthPlanCheckin::getCheckinDate)
                .collect(Collectors.toSet());

        CheckinStats stats = new CheckinStats();
        stats.setTotalCount(dates.size());

        LocalDate today = LocalDate.now();
        boolean checkedToday = dates.contains(today);
        stats.setCheckedToday(checkedToday);

        // 连续打卡：从今天（或昨天）往前数
        int streak = 0;
        LocalDate cursor = checkedToday ? today : today.minusDays(1);
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        stats.setStreak(streak);

        // 近 7 天（含今天）次数
        LocalDate weekStart = today.minusDays(6);
        long week = dates.stream().filter(d -> !d.isBefore(weekStart)).count();
        stats.setWeekCount((int) week);

        // 近 7 天打卡情况，index 0=6天前 ... 6=今天
        List<Boolean> last7 = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            last7.add(dates.contains(today.minusDays(i)));
        }
        stats.setLast7(last7);

        return stats;
    }

    @Override
    public Map<Long, CheckinStats> overview(Long userId, List<Long> planIds) {
        Map<Long, CheckinStats> map = new HashMap<>();
        if (planIds == null) return map;
        for (Long planId : planIds) {
            map.put(planId, stats(userId, planId));
        }
        return map;
    }

    @Override
    public List<String> dates(Long userId, Long planId) {
        LambdaQueryWrapper<HealthPlanCheckin> w = new LambdaQueryWrapper<>();
        w.eq(HealthPlanCheckin::getUserId, userId)
                .eq(HealthPlanCheckin::getPlanId, planId)
                .select(HealthPlanCheckin::getCheckinDate)
                .orderByAsc(HealthPlanCheckin::getCheckinDate);
        return checkinMapper.selectList(w).stream()
                .map(c -> c.getCheckinDate().toString())
                .collect(Collectors.toList());
    }

    /** 打卡后判定是否达成目标，达成则把计划标记为「已完成」 */
    private void evaluateAchievement(Long userId, Long planId) {
        HealthPlan plan = healthPlanService.getById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) return;
        if ("已完成".equals(plan.getStatus())) return;

        long count = checkinMapper.selectCount(new LambdaQueryWrapper<HealthPlanCheckin>()
                .eq(HealthPlanCheckin::getUserId, userId)
                .eq(HealthPlanCheckin::getPlanId, planId));

        boolean achieve;
        Integer target = plan.getTargetDays();
        if (target != null && target > 0) {
            achieve = count >= target;                       // 累计打卡天数达到目标
        } else {
            achieve = LocalDate.now().isAfter(plan.getEndDate()); // 无目标：已过结束日期
        }
        if (achieve) {
            plan.setStatus("已完成");
            healthPlanService.updateById(plan);
        }
    }
}

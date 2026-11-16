package com.health.healthserver.service;

import com.health.healthserver.dto.CheckinStats;

import java.util.List;
import java.util.Map;

public interface HealthPlanCheckinService {

    /** 打卡（幂等：同一用户同一计划同一天只记一次） */
    void checkin(Long userId, Long planId);

    /** 单个计划的打卡统计 */
    CheckinStats stats(Long userId, Long planId);

    /** 批量概览：planId -> 统计，用于列表一次性渲染 */
    Map<Long, CheckinStats> overview(Long userId, List<Long> planIds);

    /** 某计划的所有打卡日期（yyyy-MM-dd），用于日历展示 */
    List<String> dates(Long userId, Long planId);
}

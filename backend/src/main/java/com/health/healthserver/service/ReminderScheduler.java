package com.health.healthserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthserver.entity.Admin;
import com.health.healthserver.entity.HealthPlan;
import com.health.healthserver.mapper.AdminMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 健康提醒定时任务：
 * 每分钟扫描一次“进行中”且已到当日提醒时间、今天尚未发送过的计划，
 * 向该计划所属用户的邮箱发送提醒邮件。
 * 去重采用内存记录（planId -> 最后发送日期），重启后当天可能补发一次。
 */
@Component
public class ReminderScheduler {

    @Resource
    private HealthPlanService healthPlanService;

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private EmailService emailService;

    /** planId -> 当日已发送日期，用于去重 */
    private final Map<Long, LocalDate> sentMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 60_000)
    public void scanAndSend() {
        LocalDate today = LocalDate.now();
        List<HealthPlan> plans = healthPlanService.list(
                new LambdaQueryWrapper<HealthPlan>().eq(HealthPlan::getStatus, "进行中")
        );
        for (HealthPlan plan : plans) {
            if (plan.getRemindTime() == null || plan.getRemindTime().isBlank()) {
                continue;
            }
            LocalTime remind;
            try {
                remind = LocalTime.parse(plan.getRemindTime());
            } catch (Exception e) {
                continue;
            }
            // 未到当日提醒时间则跳过
            if (LocalTime.now().isBefore(remind)) {
                continue;
            }
            // 今天已发送过则跳过
            if (today.equals(sentMap.get(plan.getId()))) {
                continue;
            }
            Admin admin = adminMapper.selectById(plan.getUserId());
            String email = admin != null ? admin.getEmail() : null;
            if (email == null || email.isBlank()) {
                continue;
            }
            emailService.sendReminder(email, "【健康提醒】" + plan.getPlanName(), buildContent(plan));
            sentMap.put(plan.getId(), today);
        }
    }

    private String buildContent(HealthPlan plan) {
        return "<div style='font-family:Helvetica,Arial,sans-serif;line-height:1.6'>"
                + "<h3 style='color:#d4380d'>🔔 健康计划提醒</h3>"
                + "<p>你有一个健康计划已到今日提醒时间，记得完成打卡哦~</p>"
                + "<ul>"
                + "<li><b>计划名称：</b>" + plan.getPlanName() + "</li>"
                + "<li><b>计划类型：</b>" + plan.getPlanType() + "</li>"
                + "<li><b>提醒时间：</b>" + plan.getRemindTime() + "</li>"
                + (plan.getDescription() != null && !plan.getDescription().isBlank()
                ? "<li><b>描述：</b>" + plan.getDescription() + "</li>" : "")
                + "</ul>"
                + "<p style='color:#888;font-size:12px'>本邮件由健康管理系统自动发送，请勿直接回复。</p>"
                + "</div>";
    }
}

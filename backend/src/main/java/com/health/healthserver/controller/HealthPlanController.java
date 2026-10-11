package com.health.healthserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.health.healthserver.dto.HealthPlanQueryDTO;
import com.health.healthserver.entity.HealthPlan;
import com.health.healthserver.response.R;
import com.health.healthserver.service.HealthPlanService;
import com.health.healthserver.utils.UserContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.annotation.Resource;
@RestController
@RequestMapping("/health-plan")
public class HealthPlanController {

    @Resource
    private HealthPlanService healthPlanService;

    @PostMapping("/list")
    public R<PageInfo<HealthPlan>> list(@RequestBody HealthPlanQueryDTO query) {
        Long userId = UserContext.getUserId();  // 👈 后端自己拿，不信前端
        if (userId == null) return R.fail("未登录");
        LambdaQueryWrapper<HealthPlan> wrapper = new LambdaQueryWrapper<>();

        // 核心：只查当前用户的计划
        wrapper.eq(query.getUser_id() != null, HealthPlan::getUserId, query.getUser_id());

        // 模糊查询条件
        wrapper.like(query.getPlan_name() != null && !query.getPlan_name().isEmpty(),
                HealthPlan::getPlanName, query.getPlan_name());
        wrapper.eq(query.getPlan_type() != null && !query.getPlan_type().isEmpty(),
                HealthPlan::getPlanType, query.getPlan_type());
        wrapper.eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                HealthPlan::getStatus, query.getStatus());

        // 按创建时间倒序
        wrapper.orderByDesc(HealthPlan::getCreateTime);

        PageHelper.startPage(query.getPageNumber(), query.getPageSize());
        List<HealthPlan> list = healthPlanService.list(wrapper);
        PageInfo<HealthPlan> pageInfo = new PageInfo<>(list);

        return R.data(pageInfo);
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody HealthPlan plan) {
        System.out.println("====== 后端接收到的计划数据: " + plan);
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        // 👈 核心：强制设置 userId，前端传什么都无所谓
        plan.setUserId(userId);
        healthPlanService.save(plan);
        return R.success("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody HealthPlan plan) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        // 👈 核心：先查出这条数据，确认属于当前用户才能改
        HealthPlan dbPlan = healthPlanService.getById(plan.getId());
        if (dbPlan == null) return R.fail("数据不存在");
        if (!dbPlan.getUserId().equals(userId)) return R.fail("无权操作");

        plan.setUserId(userId); // 再强制设置一次，防止被篡改
        healthPlanService.updateById(plan);
        return R.success("更新成功");
    }

    @PostMapping("/del")
    public R<?> delete(@RequestParam Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        HealthPlan dbPlan = healthPlanService.getById(id);
        if (dbPlan == null) return R.fail("数据不存在");
        if (!dbPlan.getUserId().equals(userId)) return R.fail("无权操作");

        healthPlanService.removeById(id);
        return R.success("删除成功");
    }
}
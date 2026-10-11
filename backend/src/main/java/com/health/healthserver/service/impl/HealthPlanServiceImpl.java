package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.mapper.HealthPlanMapper;
import com.health.healthserver.entity.HealthPlan;
import com.health.healthserver.service.HealthPlanService;
import org.springframework.stereotype.Service;

@Service
public class HealthPlanServiceImpl extends ServiceImpl<HealthPlanMapper, HealthPlan> implements HealthPlanService {
}
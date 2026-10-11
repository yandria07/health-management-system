package com.health.healthserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthserver.entity.HealthPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthPlanMapper extends BaseMapper<HealthPlan> {
}
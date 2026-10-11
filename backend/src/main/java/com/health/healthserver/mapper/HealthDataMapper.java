package com.health.healthserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthserver.entity.HealthData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthDataMapper extends BaseMapper<HealthData> {
}
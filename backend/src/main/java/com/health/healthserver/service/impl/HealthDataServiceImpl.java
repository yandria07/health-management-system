package com.health.healthserver.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.mapper.HealthDataMapper;
import com.health.healthserver.entity.HealthData;
import com.health.healthserver.service.HealthDataService;
import org.springframework.stereotype.Service;

@Service
public class HealthDataServiceImpl extends ServiceImpl<HealthDataMapper, HealthData> implements HealthDataService {}
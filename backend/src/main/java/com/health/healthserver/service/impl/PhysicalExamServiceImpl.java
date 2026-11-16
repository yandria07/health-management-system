package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.entity.PhysicalExam;
import com.health.healthserver.mapper.PhysicalExamMapper;
import com.health.healthserver.service.PhysicalExamService;
import org.springframework.stereotype.Service;

@Service
public class PhysicalExamServiceImpl extends ServiceImpl<PhysicalExamMapper, PhysicalExam>
        implements PhysicalExamService {
}

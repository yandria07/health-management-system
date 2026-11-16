package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.entity.ExamInterpretation;
import com.health.healthserver.mapper.ExamInterpretationMapper;
import com.health.healthserver.service.ExamInterpretationService;
import org.springframework.stereotype.Service;

@Service
public class ExamInterpretationServiceImpl
        extends ServiceImpl<ExamInterpretationMapper, ExamInterpretation>
        implements ExamInterpretationService {
}

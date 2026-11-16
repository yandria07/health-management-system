package com.health.healthserver.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.mapper.AiAdviceMapper;
import com.health.healthserver.entity.AiAdvice;
import com.health.healthserver.service.AiAdviceService;
import org.springframework.stereotype.Service;

@Service
public class AiAdviceServiceImpl extends ServiceImpl<AiAdviceMapper, AiAdvice> implements AiAdviceService {}
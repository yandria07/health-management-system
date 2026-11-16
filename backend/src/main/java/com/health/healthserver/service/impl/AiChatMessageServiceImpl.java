package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.entity.AiChatMessage;
import com.health.healthserver.mapper.AiChatMessageMapper;
import com.health.healthserver.service.AiChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
        implements AiChatMessageService {
}

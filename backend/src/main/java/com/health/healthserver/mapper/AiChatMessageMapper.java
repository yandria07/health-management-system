package com.health.healthserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthserver.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}

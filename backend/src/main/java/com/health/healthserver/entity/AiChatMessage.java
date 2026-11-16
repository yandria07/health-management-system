package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    /** 角色：user（用户）或 assistant（AI） */
    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

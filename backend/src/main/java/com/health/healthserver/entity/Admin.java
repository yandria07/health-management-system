package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("admin")
@Schema(description = "管理员信息实体")
public class Admin {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;
    @Schema(description = "用户名")
    private String username;
    private String email;
    private String nickname;
    private String password;
    private Integer gender;
    private String phone;
    @TableField("userType")
    private Integer userType;
    @TableField(exist = false)
    private String confirmPassword;
}

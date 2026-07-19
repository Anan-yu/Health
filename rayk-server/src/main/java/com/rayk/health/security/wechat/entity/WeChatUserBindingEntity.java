package com.rayk.health.security.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wx_user_binding")
public class WeChatUserBindingEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String appId;
    private String openid;
    private String unionid;
    private String status;
    private LocalDateTime lastLoginAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;
}


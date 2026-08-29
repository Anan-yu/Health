package com.rayk.health.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mall_order_item")
public class MallOrderItemEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long tenantId;
    private Long orderId;
    private Long productId;
    private String productName;
    private String mainImageUrl;
    private Integer unitPriceCent;
    private Integer quantity;
    private Integer totalCent;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    private Integer version;
}

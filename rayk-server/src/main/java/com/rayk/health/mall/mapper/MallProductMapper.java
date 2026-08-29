package com.rayk.health.mall.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.mall.entity.MallProductEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** Product catalog is platform-managed and shared by customer tenants. */
@InterceptorIgnore(tenantLine = "true")
public interface MallProductMapper extends BaseMapper<MallProductEntity> {
    @Update("""
            UPDATE mall_product
            SET stock = stock - #{quantity},
                sold_count = sold_count + #{quantity},
                updated_by = #{operatorId},
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{productId}
              AND status = 'ACTIVE'
              AND deleted = 0
              AND stock >= #{quantity}
            """)
    int reserveStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("operatorId") long operatorId,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE mall_product
            SET stock = stock + #{quantity},
                sold_count = GREATEST(0, sold_count - #{quantity}),
                updated_by = #{operatorId},
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{productId} AND deleted = 0
            """)
    int releaseStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("operatorId") long operatorId,
            @Param("updatedAt") LocalDateTime updatedAt);
}

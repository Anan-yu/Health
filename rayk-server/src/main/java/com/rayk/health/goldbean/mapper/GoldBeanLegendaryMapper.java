package com.rayk.health.goldbean.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.goldbean.entity.GoldBeanLegendaryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Explicit platform-scope queries for the legendary phone allowlist. */
@Mapper
public interface GoldBeanLegendaryMapper extends BaseMapper<GoldBeanLegendaryEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM gold_member_legendary
            WHERE tenant_id = 1 AND phone_hash = #{phoneHash}
              AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    GoldBeanLegendaryEntity selectByPhoneHashForUpdate(@Param("phoneHash") String phoneHash);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM gold_member_legendary
            WHERE tenant_id = 1 AND id = #{id} AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    GoldBeanLegendaryEntity selectPlatformByIdForUpdate(@Param("id") long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM gold_member_legendary
            WHERE tenant_id = 1 AND phone_hash = #{phoneHash}
              AND status = 'ACTIVE' AND deleted = 0
            LIMIT 1
            """)
    GoldBeanLegendaryEntity selectActiveByPhoneHash(@Param("phoneHash") String phoneHash);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM gold_member_legendary
            WHERE tenant_id = 1 AND deleted = 0
            ORDER BY created_at DESC, id DESC
            LIMIT 200
            """)
    List<GoldBeanLegendaryEntity> selectPlatformList();

    @InterceptorIgnore(tenantLine = "true")
    @Insert("""
            INSERT INTO gold_member_legendary
                (id, tenant_id, phone_hash, phone_masked, status, note,
                 created_by, created_at, updated_by, updated_at, deleted, version)
            VALUES
                (#{item.id}, 1, #{item.phoneHash}, #{item.phoneMasked}, #{item.status}, #{item.note},
                 #{item.createdBy}, #{item.createdAt}, #{item.updatedBy}, #{item.updatedAt},
                 #{item.deleted}, #{item.version})
            """)
    int insertPlatform(@Param("item") GoldBeanLegendaryEntity item);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE gold_member_legendary
            SET phone_masked = #{item.phoneMasked}, status = #{item.status}, note = #{item.note},
                updated_by = #{item.updatedBy}, updated_at = #{item.updatedAt}, version = #{item.version}
            WHERE id = #{item.id} AND tenant_id = 1 AND deleted = 0
            """)
    int updatePlatform(@Param("item") GoldBeanLegendaryEntity item);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE gold_member_legendary
            SET status = 'REVOKED', updated_by = #{operatorId}, updated_at = #{updatedAt}
            WHERE id = #{id} AND tenant_id = 1 AND deleted = 0
            """)
    int revokePlatform(
            @Param("id") long id,
            @Param("operatorId") long operatorId,
            @Param("updatedAt") LocalDateTime updatedAt);
}

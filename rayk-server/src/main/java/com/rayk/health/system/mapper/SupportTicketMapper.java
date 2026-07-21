package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SupportTicketEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SupportTicketMapper extends BaseMapper<SupportTicketEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT * FROM support_ticket
            WHERE deleted = 0
            ORDER BY created_at DESC
            LIMIT #{size}
            """)
    List<SupportTicketEntity> selectPlatformTickets(@Param("size") long size);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM support_ticket WHERE id = #{id} AND deleted = 0")
    SupportTicketEntity selectPlatformTicketById(@Param("id") long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update(
            """
            UPDATE support_ticket
            SET reply = #{reply}, status = #{status}, updated_by = #{operatorId}, updated_at = #{updatedAt}
            WHERE id = #{id} AND deleted = 0
            """)
    int updatePlatformReply(
            @Param("id") long id,
            @Param("reply") String reply,
            @Param("status") String status,
            @Param("operatorId") long operatorId,
            @Param("updatedAt") LocalDateTime updatedAt);
}

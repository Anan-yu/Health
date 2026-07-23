package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SysUserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysUserMapper extends BaseMapper<SysUserEntity> {
    /**
     * 登录前需跨租户按用户名查找用户。sys_user 受租户拦截器自动过滤，
     * 此处显式忽略租户行过滤以支持认证前的全局用户名查找。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select(
            "SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUserEntity selectByUsernameIgnoringTenant(@Param("username") String username);

    /**
     * 按主键跨租户查找用户（如微信绑定回填、JWT 资料刷新等认证后但无租户上下文场景）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE id = #{userId} AND deleted = 0")
    SysUserEntity selectByIdIgnoringTenant(@Param("userId") long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE phone_hash = #{phoneHash} AND deleted = 0 LIMIT 1")
    SysUserEntity selectByPhoneHashIgnoringTenant(@Param("phoneHash") String phoneHash);

    /**
     * 跨租户列出全部有效用户（测试与健康检查场景）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE deleted = 0")
    List<SysUserEntity> selectAllIgnoringTenant();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT DISTINCT u.* FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE u.tenant_id = #{tenantId} AND u.deleted = 0
              AND r.role_code = 'DOCTOR'
            ORDER BY u.created_at, u.id
            """)
    List<SysUserEntity> selectDoctorsByTenantIgnoringTenant(@Param("tenantId") long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Update(
            """
            UPDATE sys_user
            SET display_name = #{user.displayName}, phone_masked = #{user.phoneMasked},
                phone_hash = #{user.phoneHash}, updated_by = #{user.updatedBy}, updated_at = #{user.updatedAt}
            WHERE id = #{user.id} AND tenant_id = #{user.tenantId} AND deleted = 0
            """)
    int updateDoctorIgnoringTenant(@Param("user") SysUserEntity user);

    @InterceptorIgnore(tenantLine = "true")
    @Update(
            """
            UPDATE sys_user
            SET deleted = 1, updated_by = #{operatorId}, updated_at = #{updatedAt}
            WHERE id = #{doctorId} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDeleteDoctorIgnoringTenant(
            @Param("tenantId") long tenantId,
            @Param("doctorId") long doctorId,
            @Param("operatorId") long operatorId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}

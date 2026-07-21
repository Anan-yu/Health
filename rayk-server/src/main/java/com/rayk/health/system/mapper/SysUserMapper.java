package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SysUserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 跨租户列出全部有效用户（测试与健康检查场景）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE deleted = 0")
    List<SysUserEntity> selectAllIgnoringTenant();
}

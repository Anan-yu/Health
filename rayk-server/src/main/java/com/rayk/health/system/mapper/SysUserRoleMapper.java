package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SysUserRoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserRoleMapper extends BaseMapper<SysUserRoleEntity> {
    /**
     * 查询用户拥有的有效角色编码（关联 sys_role，过滤已删除与非 ACTIVE 角色）。
     */
    @Select(
            """
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'ACTIVE'
            WHERE ur.user_id = #{userId} AND ur.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") long userId);
}

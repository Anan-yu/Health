package com.rayk.health.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.report.entity.HealthReportEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface HealthReportMapper extends BaseMapper<HealthReportEntity> {
    /** Locks one report while a missing artifact is checked and repaired. */
    @Select("SELECT * FROM health_report WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    HealthReportEntity selectByIdForUpdate(@Param("id") long id);
}


package com.rayk.health.platform.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface HealthReportRecoveryMapper {
    @InterceptorIgnore(tenantLine = "true")
    @Select(
            "SELECT id AS reportId, tenant_id AS tenantId FROM health_report "
                    + "WHERE status = 'PUBLISHED' AND deleted = 0 ORDER BY id")
    List<PublishedReportRef> selectPublishedReports();

    record PublishedReportRef(long reportId, long tenantId) {}
}

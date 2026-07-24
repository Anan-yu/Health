package com.rayk.health.followup.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.followup.entity.FollowupTaskEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FollowupTaskMapper extends BaseMapper<FollowupTaskEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select(
            "SELECT * FROM followup_task "
                    + "WHERE status = 'PENDING' AND due_date <= #{latestDate} AND deleted = 0")
    List<FollowupTaskEntity> selectReminderCandidates(
            @Param("latestDate") LocalDate latestDate);
}


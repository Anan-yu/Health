package com.rayk.health.platform.application;

import com.rayk.health.platform.mapper.PlatformOverviewMapper;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformOverviewService {
    private final PlatformOverviewMapper mapper;

    public PlatformOverviewService(PlatformOverviewMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PlatformOverviewVo overview() {
        return new PlatformOverviewVo(
                mapper.countTenants(),
                mapper.countActiveTenants(),
                mapper.countUsers(),
                mapper.countPatients(),
                mapper.countPendingReviews(),
                mapper.countPendingFollowups(),
                mapper.selectTenants());
    }
}

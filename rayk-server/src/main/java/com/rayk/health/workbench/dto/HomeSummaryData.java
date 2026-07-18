package com.rayk.health.workbench.dto;

import java.util.List;

public record HomeSummaryData(
        String workbench,
        String greeting,
        List<HomeMetric> metrics,
        String disclaimer) {}


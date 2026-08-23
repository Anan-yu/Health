package com.rayk.health.system.vo;

public record ReleaseVersionVo(
        String releaseId,
        String gitCommit,
        boolean gitDirty,
        String buildTime,
        String databaseMigration,
        String frontendH5Sha256,
        String frontendMpWeixinDevSha256,
        String frontendMpWeixinProdLanSha256) {}

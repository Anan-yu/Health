CREATE TABLE privacy_consent (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户标识',
  patient_id BIGINT NOT NULL COMMENT '客户主键',
  consent_type VARCHAR(50) NOT NULL COMMENT '授权类型',
  policy_version VARCHAR(30) NOT NULL COMMENT '隐私政策版本',
  consented TINYINT NOT NULL DEFAULT 1 COMMENT '是否同意',
  consented_at DATETIME NULL COMMENT '同意时间',
  revoked_at DATETIME NULL COMMENT '撤回时间',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_by BIGINT NOT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  KEY idx_consent_patient (tenant_id, patient_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐私授权记录表';

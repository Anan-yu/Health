CREATE TABLE support_ticket (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户标识',
  user_id BIGINT NOT NULL COMMENT '提交用户',
  category VARCHAR(30) NOT NULL COMMENT '反馈分类',
  content VARCHAR(1000) NOT NULL COMMENT '反馈内容',
  contact VARCHAR(100) NULL COMMENT '联系方式',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  reply VARCHAR(1000) NULL COMMENT '处理回复',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_by BIGINT NOT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  KEY idx_support_ticket_user (tenant_id, user_id, created_at),
  KEY idx_support_ticket_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帮助与反馈工单';

CREATE TABLE wx_user_binding (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户标识',
  user_id BIGINT NOT NULL COMMENT '系统用户主键',
  app_id VARCHAR(64) NOT NULL COMMENT '微信小程序AppID',
  openid VARCHAR(128) NOT NULL COMMENT '微信OpenID',
  unionid VARCHAR(128) NULL COMMENT '微信UnionID',
  status VARCHAR(20) NOT NULL COMMENT '绑定状态',
  last_login_at DATETIME NULL COMMENT '最近微信登录时间',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_by BIGINT NOT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wx_app_openid (app_id, openid),
  UNIQUE KEY uk_wx_app_user (app_id, user_id),
  KEY idx_wx_tenant_user (tenant_id, user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信小程序用户绑定表';

ALTER TABLE lab_report_file
  ADD KEY idx_file_status (tenant_id, status, created_at);


CREATE TABLE sys_tenant (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
  tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称', status VARCHAR(20) NOT NULL COMMENT '状态', service_plan VARCHAR(30) NOT NULL COMMENT '服务套餐',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_tenant_code (tenant_code), KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE TABLE sys_user (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', username VARCHAR(50) NOT NULL COMMENT '登录名', password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
  display_name VARCHAR(50) NOT NULL COMMENT '显示名称', phone_masked VARCHAR(30) NULL COMMENT '脱敏手机号', status VARCHAR(20) NOT NULL COMMENT '状态',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_tenant_username (tenant_id, username), KEY idx_user_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE sys_role (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', role_code VARCHAR(50) NOT NULL COMMENT '角色编码', role_name VARCHAR(50) NOT NULL COMMENT '角色名称', status VARCHAR(20) NOT NULL COMMENT '状态',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_role_code (role_code), KEY idx_role_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_permission (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', permission_code VARCHAR(100) NOT NULL COMMENT '权限编码', permission_name VARCHAR(100) NOT NULL COMMENT '权限名称', module_code VARCHAR(50) NOT NULL COMMENT '模块编码',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_permission_code (permission_code), KEY idx_permission_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', user_id BIGINT NOT NULL COMMENT '用户主键', role_id BIGINT NOT NULL COMMENT '角色主键',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_user_role (tenant_id, user_id, role_id), KEY idx_user_role_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE sys_role_permission (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', role_id BIGINT NOT NULL COMMENT '角色主键', permission_id BIGINT NOT NULL COMMENT '权限主键',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_role_permission (tenant_id, role_id, permission_id), KEY idx_role_permission_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE sys_user_workbench (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', user_id BIGINT NOT NULL COMMENT '用户主键', workbench_code VARCHAR(50) NOT NULL COMMENT '工作台编码', is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认工作台',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_user_workbench (tenant_id, user_id, workbench_code), KEY idx_workbench_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户工作台表';

CREATE TABLE sys_user_customer_scope (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', user_id BIGINT NOT NULL COMMENT '机构人员用户主键', patient_id BIGINT NOT NULL COMMENT '授权客户主键', scope_type VARCHAR(30) NOT NULL COMMENT '授权范围类型',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_user_patient_scope (tenant_id, user_id, patient_id), KEY idx_scope_patient (tenant_id, patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户客户数据授权表';

CREATE TABLE health_patient (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', user_id BIGINT NULL COMMENT '关联C端用户主键', name VARCHAR(50) NOT NULL COMMENT '客户姓名', gender VARCHAR(20) NOT NULL COMMENT '性别', birth_date DATE NULL COMMENT '出生日期', phone_masked VARCHAR(30) NULL COMMENT '脱敏手机号',
  assigned_doctor_id BIGINT NULL COMMENT '分配医生主键', assigned_manager_id BIGINT NULL COMMENT '分配健康管理师主键', status VARCHAR(20) NOT NULL COMMENT '客户状态',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_patient_tenant_status (tenant_id, status), KEY idx_patient_doctor (tenant_id, assigned_doctor_id), KEY idx_patient_manager (tenant_id, assigned_manager_id), KEY idx_patient_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康客户档案表';

CREATE TABLE health_profile (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', patient_id BIGINT NOT NULL COMMENT '客户主键', height_cm DECIMAL(6,2) NULL COMMENT '身高厘米', weight_kg DECIMAL(6,2) NULL COMMENT '体重千克', blood_type VARCHAR(10) NULL COMMENT '血型', lifestyle_summary VARCHAR(1000) NULL COMMENT '生活方式摘要', profile_completeness INT NOT NULL DEFAULT 0 COMMENT '档案完整度',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_profile_patient (tenant_id, patient_id), KEY idx_profile_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户健康档案详情表';

CREATE TABLE lab_report (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', patient_id BIGINT NOT NULL COMMENT '客户主键', report_name VARCHAR(100) NOT NULL COMMENT '报告名称', report_date DATE NOT NULL COMMENT '报告日期', status VARCHAR(30) NOT NULL COMMENT '报告状态', source_type VARCHAR(30) NOT NULL COMMENT '来源类型', ocr_snapshot JSON NULL COMMENT 'OCR原始结果快照', failure_reason VARCHAR(500) NULL COMMENT '失败原因',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_report_tenant_status (tenant_id, status), KEY idx_report_patient_date (tenant_id, patient_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验报告表';

CREATE TABLE lab_report_file (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', report_id BIGINT NOT NULL COMMENT '检验报告主键', bucket_name VARCHAR(100) NOT NULL COMMENT '对象桶名称', object_path VARCHAR(500) NOT NULL COMMENT '对象路径', original_name VARCHAR(255) NOT NULL COMMENT '原文件名', mime_type VARCHAR(100) NOT NULL COMMENT 'MIME类型', file_size BIGINT NOT NULL COMMENT '文件字节数', sha256 VARCHAR(64) NULL COMMENT '文件摘要', status VARCHAR(20) NOT NULL COMMENT '文件状态',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_file_report (tenant_id, report_id), UNIQUE KEY uk_file_object (tenant_id, bucket_name, object_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验报告文件元数据表';

CREATE TABLE indicator_dict (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', indicator_code VARCHAR(80) NOT NULL COMMENT '指标编码', indicator_name VARCHAR(100) NOT NULL COMMENT '指标名称', standard_unit VARCHAR(30) NOT NULL COMMENT '标准单位', category_code VARCHAR(50) NOT NULL COMMENT '指标分类', enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_indicator_code (tenant_id, indicator_code), KEY idx_indicator_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标字典表';

CREATE TABLE indicator_alias (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', indicator_id BIGINT NOT NULL COMMENT '指标字典主键', alias_name VARCHAR(100) NOT NULL COMMENT '指标别名', source VARCHAR(50) NOT NULL COMMENT '别名来源',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_indicator_alias (tenant_id, alias_name), KEY idx_alias_indicator (tenant_id, indicator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标别名表';

CREATE TABLE indicator_value (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', report_id BIGINT NOT NULL COMMENT '检验报告主键', patient_id BIGINT NOT NULL COMMENT '客户主键', indicator_code VARCHAR(80) NOT NULL COMMENT '指标编码', indicator_name VARCHAR(100) NOT NULL COMMENT '指标名称', value DECIMAL(18,6) NOT NULL COMMENT '指标数值', unit VARCHAR(30) NOT NULL COMMENT '指标单位', reference_low DECIMAL(18,6) NULL COMMENT '参考下限', reference_high DECIMAL(18,6) NULL COMMENT '参考上限', abnormal_flag VARCHAR(20) NOT NULL COMMENT '异常标识', manually_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '是否人工确认',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_value_report (tenant_id, report_id), KEY idx_value_trend (tenant_id, patient_id, indicator_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验指标值表';

CREATE TABLE ai_task (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', report_id BIGINT NOT NULL COMMENT '检验报告主键', patient_id BIGINT NOT NULL COMMENT '客户主键', task_code VARCHAR(50) NOT NULL COMMENT 'AI任务编码', task_type VARCHAR(50) NOT NULL COMMENT 'AI任务类型', status VARCHAR(20) NOT NULL COMMENT '任务状态', error_message VARCHAR(500) NULL COMMENT '错误信息', started_at DATETIME NULL COMMENT '开始时间', finished_at DATETIME NULL COMMENT '结束时间',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_ai_task_code (tenant_id, task_code), KEY idx_ai_task_status (tenant_id, status), KEY idx_ai_task_report (tenant_id, report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';

CREATE TABLE health_assessment (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', ai_task_id BIGINT NOT NULL COMMENT 'AI任务主键', report_id BIGINT NOT NULL COMMENT '检验报告主键', patient_id BIGINT NOT NULL COMMENT '客户主键', model_version VARCHAR(50) NOT NULL COMMENT '模型版本', status VARCHAR(20) NOT NULL COMMENT '评估状态', overall_risk_level VARCHAR(20) NOT NULL COMMENT '整体风险等级', result_snapshot JSON NOT NULL COMMENT 'AI评估结果快照', disclaimer VARCHAR(255) NOT NULL COMMENT '健康管理免责声明',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_assessment_patient (tenant_id, patient_id, created_at), KEY idx_assessment_report (tenant_id, report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评估表';

CREATE TABLE health_model_result (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', assessment_id BIGINT NOT NULL COMMENT '评估主键', model_code VARCHAR(80) NOT NULL COMMENT '模型编码', model_name VARCHAR(100) NOT NULL COMMENT '模型名称', score DECIMAL(5,2) NOT NULL COMMENT '模型分数', risk_level VARCHAR(20) NOT NULL COMMENT '风险等级', evidence_snapshot JSON NOT NULL COMMENT '证据快照', missing_snapshot JSON NOT NULL COMMENT '缺失指标快照', recommendation_snapshot JSON NOT NULL COMMENT '建议快照',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_model_assessment (tenant_id, assessment_id), KEY idx_model_code (tenant_id, model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评估模型结果表';

CREATE TABLE assessment_review (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', assessment_id BIGINT NOT NULL COMMENT '评估主键', patient_id BIGINT NOT NULL COMMENT '客户主键', reviewer_id BIGINT NULL COMMENT '审核医生主键', status VARCHAR(20) NOT NULL COMMENT '审核状态', review_opinion VARCHAR(1000) NULL COMMENT '审核意见', reviewed_at DATETIME NULL COMMENT '审核时间',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_review_status (tenant_id, status), KEY idx_review_assessment (tenant_id, assessment_id), KEY idx_review_patient (tenant_id, patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评估人工审核表';

CREATE TABLE health_report (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', patient_id BIGINT NOT NULL COMMENT '客户主键', assessment_id BIGINT NOT NULL COMMENT '评估主键', report_no VARCHAR(50) NOT NULL COMMENT '健康报告编号', title VARCHAR(200) NOT NULL COMMENT '报告标题', status VARCHAR(20) NOT NULL COMMENT '报告状态', summary VARCHAR(2000) NOT NULL COMMENT '报告摘要', doctor_opinion VARCHAR(1000) NULL COMMENT '医生审核意见', disclaimer VARCHAR(255) NOT NULL COMMENT '健康管理免责声明', published_at DATETIME NULL COMMENT '发布时间', published_by BIGINT NULL COMMENT '发布人',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_health_report_no (tenant_id, report_no), KEY idx_health_report_patient (tenant_id, patient_id, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已发布健康报告表';

CREATE TABLE health_report_version (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', health_report_id BIGINT NOT NULL COMMENT '健康报告主键', version_no INT NOT NULL COMMENT '报告版本号', content_snapshot JSON NOT NULL COMMENT '报告内容快照', object_path VARCHAR(500) NULL COMMENT 'PDF对象路径',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), UNIQUE KEY uk_report_version (tenant_id, health_report_id, version_no), KEY idx_report_version_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康报告版本表';

CREATE TABLE followup_plan (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', patient_id BIGINT NOT NULL COMMENT '客户主键', plan_name VARCHAR(100) NOT NULL COMMENT '随访计划名称', start_date DATE NOT NULL COMMENT '开始日期', end_date DATE NULL COMMENT '结束日期', status VARCHAR(20) NOT NULL COMMENT '计划状态',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_followup_plan_patient (tenant_id, patient_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访计划表';

CREATE TABLE followup_task (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', patient_id BIGINT NOT NULL COMMENT '客户主键', assignee_id BIGINT NULL COMMENT '执行人主键', title VARCHAR(100) NOT NULL COMMENT '随访标题', content VARCHAR(1000) NOT NULL COMMENT '随访内容', due_date DATE NOT NULL COMMENT '计划完成日期', status VARCHAR(20) NOT NULL COMMENT '随访状态', feedback VARCHAR(1000) NULL COMMENT '客户反馈', completed_at DATETIME NULL COMMENT '完成时间',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_followup_task_status (tenant_id, status, due_date), KEY idx_followup_task_patient (tenant_id, patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访任务表';

CREATE TABLE followup_feedback (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', followup_task_id BIGINT NOT NULL COMMENT '随访任务主键', patient_id BIGINT NOT NULL COMMENT '客户主键', feedback_content VARCHAR(1000) NOT NULL COMMENT '反馈内容', submitted_at DATETIME NOT NULL COMMENT '提交时间',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_feedback_task (tenant_id, followup_task_id), KEY idx_feedback_patient (tenant_id, patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访反馈表';

CREATE TABLE operation_audit_log (
  id BIGINT NOT NULL COMMENT '主键', tenant_id BIGINT NOT NULL COMMENT '租户标识', operator_id BIGINT NOT NULL COMMENT '操作用户主键', operation_type VARCHAR(80) NOT NULL COMMENT '操作类型', resource_type VARCHAR(80) NOT NULL COMMENT '资源类型', resource_id VARCHAR(80) NULL COMMENT '资源标识', request_id VARCHAR(80) NOT NULL COMMENT '请求链路标识', result VARCHAR(20) NOT NULL COMMENT '操作结果', detail_masked VARCHAR(1000) NULL COMMENT '脱敏操作详情',
  created_by BIGINT NOT NULL COMMENT '创建人', created_at DATETIME NOT NULL COMMENT '创建时间', updated_by BIGINT NOT NULL COMMENT '更新人', updated_at DATETIME NOT NULL COMMENT '更新时间', deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id), KEY idx_audit_tenant_time (tenant_id, created_at), KEY idx_audit_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';


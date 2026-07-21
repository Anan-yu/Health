-- H2 兼容测试 schema（MySQL 模式）
-- 仅包含多租户隔离测试所需的核心表

CREATE TABLE sys_tenant (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, tenant_code VARCHAR(50) NOT NULL,
  tenant_name VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL, service_plan VARCHAR(30) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_user (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, username VARCHAR(50) NOT NULL, password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(50) NOT NULL, phone_masked VARCHAR(30) NULL, status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_role (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, role_code VARCHAR(50) NOT NULL, role_name VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_permission (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, permission_code VARCHAR(100) NOT NULL, permission_name VARCHAR(100) NOT NULL, module_code VARCHAR(50) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_role_permission (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_user_workbench (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, workbench_code VARCHAR(50) NOT NULL, is_default TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE sys_user_customer_scope (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, scope_type VARCHAR(30) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_patient (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, user_id BIGINT NULL, name VARCHAR(50) NOT NULL, gender VARCHAR(20) NOT NULL, birth_date DATE NULL, phone_masked VARCHAR(30) NULL,
  assigned_doctor_id BIGINT NULL, assigned_manager_id BIGINT NULL, status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_profile (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, height_cm DECIMAL(6,2) NULL, weight_kg DECIMAL(6,2) NULL, blood_type VARCHAR(10) NULL, lifestyle_summary VARCHAR(1000) NULL, profile_completeness INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE lab_report (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, report_name VARCHAR(100) NOT NULL, report_date DATE NOT NULL, status VARCHAR(30) NOT NULL, source_type VARCHAR(30) NOT NULL, ocr_snapshot CLOB NULL, failure_reason VARCHAR(500) NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE lab_report_file (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, report_id BIGINT NOT NULL, bucket_name VARCHAR(100) NOT NULL, object_path VARCHAR(500) NOT NULL, original_name VARCHAR(255) NOT NULL, mime_type VARCHAR(100) NOT NULL, file_size BIGINT NOT NULL, sha256 VARCHAR(64) NULL, status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE indicator_dict (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, indicator_code VARCHAR(80) NOT NULL, indicator_name VARCHAR(100) NOT NULL, standard_unit VARCHAR(30) NOT NULL, category_code VARCHAR(50) NOT NULL, enabled TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE indicator_alias (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, indicator_id BIGINT NOT NULL, alias_name VARCHAR(100) NOT NULL, source VARCHAR(50) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE indicator_value (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, report_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, indicator_code VARCHAR(80) NOT NULL, indicator_name VARCHAR(100) NOT NULL, `value` DECIMAL(18,6) NOT NULL, unit VARCHAR(30) NOT NULL, reference_low DECIMAL(18,6) NULL, reference_high DECIMAL(18,6) NULL, abnormal_flag VARCHAR(20) NOT NULL, manually_confirmed TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE ai_task (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, report_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, task_code VARCHAR(50) NOT NULL, task_type VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL, error_message VARCHAR(500) NULL, started_at DATETIME NULL, finished_at DATETIME NULL,
  engine_version VARCHAR(50) NULL, confidence DECIMAL(5,2) NULL, result_snapshot CLOB NULL, attempt_count INT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_assessment (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, ai_task_id BIGINT NOT NULL, report_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, model_version VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL, overall_risk_level VARCHAR(20) NOT NULL, result_snapshot CLOB NOT NULL, disclaimer VARCHAR(255) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_model_result (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, assessment_id BIGINT NOT NULL, model_code VARCHAR(80) NOT NULL, model_name VARCHAR(100) NOT NULL, score DECIMAL(5,2) NOT NULL, risk_level VARCHAR(20) NOT NULL, evidence_snapshot CLOB NOT NULL, missing_snapshot CLOB NOT NULL, recommendation_snapshot CLOB NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE assessment_review (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, assessment_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, reviewer_id BIGINT NULL, status VARCHAR(20) NOT NULL, review_opinion VARCHAR(1000) NULL, reviewed_at DATETIME NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_report (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, assessment_id BIGINT NOT NULL, report_no VARCHAR(50) NOT NULL, title VARCHAR(200) NOT NULL, status VARCHAR(20) NOT NULL, summary VARCHAR(2000) NOT NULL, doctor_opinion VARCHAR(1000) NULL, disclaimer VARCHAR(255) NOT NULL, published_at DATETIME NULL, published_by BIGINT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE health_report_version (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, health_report_id BIGINT NOT NULL, version_no INT NOT NULL, content_snapshot CLOB NOT NULL, object_path VARCHAR(500) NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE followup_plan (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, plan_name VARCHAR(100) NOT NULL, start_date DATE NOT NULL, end_date DATE NULL, status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE followup_task (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, plan_id BIGINT NULL, assignee_id BIGINT NULL, title VARCHAR(100) NOT NULL, content VARCHAR(1000) NOT NULL, due_date DATE NOT NULL, status VARCHAR(20) NOT NULL, feedback VARCHAR(1000) NULL, completed_at DATETIME NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE followup_feedback (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, followup_task_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, feedback_content VARCHAR(1000) NOT NULL, submitted_at DATETIME NOT NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE privacy_consent (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, patient_id BIGINT NOT NULL, consent_type VARCHAR(50) NOT NULL, policy_version VARCHAR(30) NOT NULL, consented TINYINT NOT NULL DEFAULT 1, consented_at DATETIME NULL, revoked_at DATETIME NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE operation_audit_log (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, operator_id BIGINT NOT NULL, operation_type VARCHAR(80) NOT NULL, resource_type VARCHAR(80) NOT NULL, resource_id VARCHAR(80) NULL, request_id VARCHAR(80) NOT NULL, result VARCHAR(20) NOT NULL, detail_masked VARCHAR(1000) NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE wx_user_binding (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, app_id VARCHAR(50) NOT NULL, openid VARCHAR(100) NOT NULL, unionid VARCHAR(100) NULL,
  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, updated_by BIGINT NOT NULL, updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

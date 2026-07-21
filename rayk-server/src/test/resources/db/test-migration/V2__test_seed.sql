-- 测试种子数据：两个租户，用于验证跨租户隔离

INSERT INTO sys_tenant VALUES
(1, 1, 'PLATFORM', 'RayK平台', 'ACTIVE', 'PLATFORM', 10001, NOW(), 10001, NOW(), 0, 0),
(20001, 20001, 'TENANT_A', '测试机构A', 'ACTIVE', 'DEVELOPMENT', 10001, NOW(), 10001, NOW(), 0, 0),
(20002, 20002, 'TENANT_B', '测试机构B', 'ACTIVE', 'DEVELOPMENT', 10001, NOW(), 10001, NOW(), 0, 0);

-- 租户 A 客户
INSERT INTO health_patient VALUES
(30001, 20001, 10005, '租户A客户张三', 'FEMALE', '1988-05-20', '138****0001', 10003, 10004, 'ACTIVE', 10004, NOW(), 10004, NOW(), 0, 0),
(30002, 20001, NULL, '租户A客户李四', 'MALE', '1975-11-03', '138****0002', 10003, NULL, 'ACTIVE', 10004, NOW(), 10004, NOW(), 0, 0);

-- 租户 B 客户
INSERT INTO health_patient VALUES
(40001, 20002, 60003, '租户B客户王五', 'MALE', '1990-03-15', '139****0001', 60002, NULL, 'ACTIVE', 60001, NOW(), 60001, NOW(), 0, 0);

-- 合成测试客户明确授权专业人员协作，用于验证授权范围内的租户与角色隔离。
INSERT INTO privacy_consent VALUES
(41001, 20001, 30001, 'DATA_SHARING', 'TEST_1.0', 1, NOW(), NULL, 10005, NOW(), 10005, NOW(), 0, 0),
(41002, 20001, 30002, 'DATA_SHARING', 'TEST_1.0', 1, NOW(), NULL, 10005, NOW(), 10005, NOW(), 0, 0),
(41003, 20002, 40001, 'DATA_SHARING', 'TEST_1.0', 1, NOW(), NULL, 60003, NOW(), 60003, NOW(), 0, 0);

-- 租户 A 检验报告
INSERT INTO lab_report VALUES
(50001, 20001, 30001, '租户A生化报告', '2026-07-01', 'CONFIRMED', 'DEVELOPMENT_SEED', NULL, NULL, 10004, NOW(), 10003, NOW(), 0, 0);

-- 租户 B 检验报告
INSERT INTO lab_report VALUES
(60001, 20002, 40001, '租户B生化报告', '2026-07-10', 'CONFIRMED', 'DEVELOPMENT_SEED', NULL, NULL, 60001, NOW(), 60002, NOW(), 0, 0);

-- 租户 A 指标值
INSERT INTO indicator_value VALUES
(51001, 20001, 50001, 30001, 'fasting_glucose', '空腹血糖', 6.200000, 'mmol/L', 3.900000, 6.100000, 'HIGH', 1, 10004, NOW(), 10004, NOW(), 0, 0);

-- 租户 B 指标值
INSERT INTO indicator_value VALUES
(61001, 20002, 60001, 40001, 'fasting_glucose', '空腹血糖', 5.400000, 'mmol/L', 3.900000, 6.100000, 'NORMAL', 1, 60001, NOW(), 60002, NOW(), 0, 0);

-- 租户 A 随访任务
INSERT INTO followup_task
(id, tenant_id, patient_id, plan_id, assignee_id, title, content, due_date, status, feedback, completed_at,
 created_by, created_at, updated_by, updated_at, deleted, version) VALUES
(52001, 20001, 30001, NULL, 10004, '租户A月度随访', '月度健康跟踪。', DATEADD('DAY', 7, CURRENT_DATE), 'PENDING', NULL, NULL, 10003, NOW(), 10004, NOW(), 0, 0);

-- 租户 B 随访任务
INSERT INTO followup_task
(id, tenant_id, patient_id, plan_id, assignee_id, title, content, due_date, status, feedback, completed_at,
 created_by, created_at, updated_by, updated_at, deleted, version) VALUES
(62001, 20002, 40001, NULL, 60002, '租户B月度随访', '月度健康跟踪。', DATEADD('DAY', 30, CURRENT_DATE), 'PENDING', NULL, NULL, 60001, NOW(), 60001, NOW(), 0, 0);

-- 租户 A AI 任务
INSERT INTO ai_task VALUES
(53001, 20001, 50001, 30001, 'OCR_TASK_A_001', 'LAB_REPORT_OCR', 'SUCCESS', NULL, NOW(), NOW(), 'PaddleOCR-3.7.0', 97.22, NULL, 1, 10004, NOW(), 10004, NOW(), 0, 0);

-- 租户 B AI 任务
INSERT INTO ai_task VALUES
(63001, 20002, 60001, 40001, 'OCR_TASK_B_001', 'LAB_REPORT_OCR', 'SUCCESS', NULL, NOW(), NOW(), 'PaddleOCR-3.7.0', 95.00, NULL, 1, 60001, NOW(), 60002, NOW(), 0, 0);

-- 租户 A 健康评估
INSERT INTO health_assessment VALUES
(54001, 20001, 53001, 50001, 30001, 'DEMO_1.0.0', 'SUCCESS', 'ATTENTION', '{"results":[]}', '该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。', 10004, NOW(), 10004, NOW(), 0, 0);

-- 租户 B 健康评估
INSERT INTO health_assessment VALUES
(64001, 20002, 63001, 60001, 40001, 'DEMO_1.0.0', 'SUCCESS', 'LOW', '{"results":[]}', '该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。', 60001, NOW(), 60002, NOW(), 0, 0);

-- 租户 A 审核
INSERT INTO assessment_review VALUES
(55001, 20001, 54001, 30001, 10003, 'APPROVED', '同意发布。', NOW(), 10004, NOW(), 10003, NOW(), 0, 0);

-- 租户 B 审核
INSERT INTO assessment_review VALUES
(65001, 20002, 64001, 40001, 60002, 'WAITING_REVIEW', NULL, NULL, 60001, NOW(), 60001, NOW(), 0, 0);

-- 租户 A 已发布健康报告
INSERT INTO health_report VALUES
(56001, 20001, 30001, 54001, 'HR_A_001', '租户A客户健康管理报告', 'PUBLISHED', '演示摘要。', '同意发布。', '该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。', NOW(), 10003, 10003, NOW(), 10003, NOW(), 0, 0);

-- 租户 B 已发布健康报告
INSERT INTO health_report VALUES
(66001, 20002, 40001, 64001, 'HR_B_001', '租户B客户健康管理报告', 'PUBLISHED', '演示摘要。', '同意发布。', '该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。', NOW(), 60002, 60001, NOW(), 60002, NOW(), 0, 0);

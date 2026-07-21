-- V5: 添加第二个测试租户，用于多租户隔离自动化测试
-- 租户 B (tenant_id=20002) 拥有独立的用户、客户和业务数据，
-- 测试用例验证租户 A (20001) 无法访问租户 B 的任何数据。

SET @now = NOW();
SET @hash = '$2b$12$a3HfX53cvu1FWiTpSPjlh.o3SE16baDRxiMxEHjejlP4fLbG3OYXC';

-- V1 created role_code as globally unique. Role codes are tenant-scoped, so
-- replace that constraint before adding the second tenant's standard roles.
ALTER TABLE sys_role
  DROP INDEX uk_role_code,
  ADD UNIQUE KEY uk_role_tenant_code (tenant_id, role_code);

-- 租户 B
INSERT INTO sys_tenant VALUES
(20002, 20002, 'TENANT_B', '隔离测试机构B', 'ACTIVE', 'DEVELOPMENT', 10001, @now, 10001, @now, 0, 0);

-- 租户 B 用户
INSERT INTO sys_user VALUES
(60001, 20002, 'tenant_b_admin', @hash, '机构B管理员', '139****0001', 'ACTIVE', 10001, @now, 10001, @now, 0, 0),
(60002, 20002, 'tenant_b_doctor', @hash, '机构B医生', '139****0002', 'ACTIVE', 10001, @now, 10001, @now, 0, 0),
(60003, 20002, 'tenant_b_customer', @hash, '机构B客户', '139****0003', 'ACTIVE', 10001, @now, 10001, @now, 0, 0);

-- 租户 B 角色
INSERT INTO sys_role VALUES
(61001, 20002, 'TENANT_ADMIN', '机构管理员', 'ACTIVE', 10001, @now, 10001, @now, 0, 0),
(61002, 20002, 'DOCTOR', '医生', 'ACTIVE', 10001, @now, 10001, @now, 0, 0),
(61003, 20002, 'CUSTOMER', '普通客户', 'ACTIVE', 10001, @now, 10001, @now, 0, 0);

-- 租户 B 用户角色关联
INSERT INTO sys_user_role VALUES
(62001, 20002, 60001, 61001, 10001, @now, 10001, @now, 0, 0),
(62002, 20002, 60002, 61002, 10001, @now, 10001, @now, 0, 0),
(62003, 20002, 60003, 61003, 10001, @now, 10001, @now, 0, 0);

-- 租户 B 用户工作台
INSERT INTO sys_user_workbench VALUES
(63001, 20002, 60001, 'TENANT_ADMIN', 1, 10001, @now, 10001, @now, 0, 0),
(63002, 20002, 60002, 'DOCTOR', 1, 10001, @now, 10001, @now, 0, 0),
(63003, 20002, 60003, 'CUSTOMER', 1, 10001, @now, 10001, @now, 0, 0);

-- 租户 B 客户
INSERT INTO health_patient VALUES
(64001, 20002, 60003, '机构B测试客户', 'MALE', '1990-03-15', '139****0003', 60002, NULL, 'ACTIVE', 60001, @now, 60001, @now, 0, 0);

-- 租户 B 客户数据授权
INSERT INTO sys_user_customer_scope VALUES
(65001, 20002, 60002, 64001, 'DOCTOR_ASSIGNED', 60001, @now, 60001, @now, 0, 0);

-- 租户 B 检验报告（已确认状态）
INSERT INTO lab_report VALUES
(66001, 20002, 64001, '机构B生化检验报告', '2026-07-10', 'CONFIRMED', 'DEVELOPMENT_SEED', NULL, NULL, 60001, @now, 60002, @now, 0, 0);

-- 租户 B 指标值
INSERT INTO indicator_value VALUES
(67001, 20002, 66001, 64001, 'fasting_glucose', '空腹血糖', 5.400000, 'mmol/L', 3.900000, 6.100000, 'NORMAL', 1, 60001, @now, 60002, @now, 0, 0),
(67002, 20002, 66001, 64001, 'crp', 'C反应蛋白', 1.200000, 'mg/L', NULL, 3.000000, 'NORMAL', 1, 60001, @now, 60002, @now, 0, 0);

-- 租户 B 随访任务
INSERT INTO followup_task VALUES
(68001, 20002, 64001, 60002, '机构B月度随访', '月度健康状态跟踪。', DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'PENDING', NULL, NULL, 60001, @now, 60001, @now, 0, 0);

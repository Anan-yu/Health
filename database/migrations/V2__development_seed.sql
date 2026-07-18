SET @now = NOW();
SET @hash = '$2b$12$a3HfX53cvu1FWiTpSPjlh.o3SE16baDRxiMxEHjejlP4fLbG3OYXC';

INSERT INTO sys_tenant VALUES
(1,1,'PLATFORM','RayK平台','ACTIVE','PLATFORM',1,@now,1,@now,0,0),
(20001,20001,'RAYK_DEMO','RayK测试健康中心','ACTIVE','DEVELOPMENT',10001,@now,10001,@now,0,0);

INSERT INTO sys_user VALUES
(10001,1,'platform_admin',@hash,'平台管理员',NULL,'ACTIVE',10001,@now,10001,@now,0,0),
(10002,20001,'tenant_admin',@hash,'机构管理员','138****0002','ACTIVE',10001,@now,10001,@now,0,0),
(10003,20001,'doctor',@hash,'测试医生','138****0003','ACTIVE',10001,@now,10001,@now,0,0),
(10004,20001,'health_manager',@hash,'测试健康管理师','138****0004','ACTIVE',10001,@now,10001,@now,0,0),
(10005,20001,'customer',@hash,'测试客户','138****0005','ACTIVE',10001,@now,10001,@now,0,0);

INSERT INTO sys_role VALUES
(11001,1,'PLATFORM_ADMIN','平台管理员','ACTIVE',10001,@now,10001,@now,0,0),
(11002,20001,'TENANT_ADMIN','机构管理员','ACTIVE',10001,@now,10001,@now,0,0),
(11003,20001,'DOCTOR','医生','ACTIVE',10001,@now,10001,@now,0,0),
(11004,20001,'HEALTH_MANAGER','健康管理师','ACTIVE',10001,@now,10001,@now,0,0),
(11005,20001,'CUSTOMER','普通客户','ACTIVE',10001,@now,10001,@now,0,0);

INSERT INTO sys_permission VALUES
(12001,1,'platform:tenant:list','机构列表','PLATFORM',10001,@now,10001,@now,0,0),
(12002,20001,'patient:list','客户列表','PATIENT',10001,@now,10001,@now,0,0),
(12003,20001,'patient:create','创建客户','PATIENT',10001,@now,10001,@now,0,0),
(12004,20001,'lab-report:manage','检验报告管理','LAB_REPORT',10001,@now,10001,@now,0,0),
(12005,20001,'indicator:confirm','指标确认','INDICATOR',10001,@now,10001,@now,0,0),
(12006,20001,'assessment:create','创建AI评估','ASSESSMENT',10001,@now,10001,@now,0,0),
(12007,20001,'assessment:review','医生审核','REVIEW',10001,@now,10001,@now,0,0),
(12008,20001,'report:publish','发布健康报告','REPORT',10001,@now,10001,@now,0,0),
(12009,20001,'followup:manage','随访管理','FOLLOWUP',10001,@now,10001,@now,0,0),
(12010,20001,'self:health-record','个人健康档案','SELF',10001,@now,10001,@now,0,0);

INSERT INTO sys_user_role VALUES
(13001,1,10001,11001,10001,@now,10001,@now,0,0),
(13002,20001,10002,11002,10001,@now,10001,@now,0,0),
(13003,20001,10003,11003,10001,@now,10001,@now,0,0),
(13004,20001,10004,11004,10001,@now,10001,@now,0,0),
(13005,20001,10005,11005,10001,@now,10001,@now,0,0),
(13006,20001,10003,11005,10001,@now,10001,@now,0,0);

INSERT INTO sys_user_workbench VALUES
(14001,1,10001,'PLATFORM_ADMIN',1,10001,@now,10001,@now,0,0),
(14002,20001,10002,'TENANT_ADMIN',1,10001,@now,10001,@now,0,0),
(14003,20001,10003,'DOCTOR',1,10001,@now,10001,@now,0,0),
(14004,20001,10003,'CUSTOMER',0,10001,@now,10001,@now,0,0),
(14005,20001,10004,'HEALTH_MANAGER',1,10001,@now,10001,@now,0,0),
(14006,20001,10005,'CUSTOMER',1,10001,@now,10001,@now,0,0);

INSERT INTO health_patient VALUES
(30001,20001,10005,'测试客户','FEMALE','1988-05-20','138****0005',10003,10004,'ACTIVE',10004,@now,10004,@now,0,0);
INSERT INTO health_profile VALUES
(31001,20001,30001,165.00,60.00,'A','开发测试数据：规律作息，偶尔运动。',80,10004,@now,10004,@now,0,0);
INSERT INTO sys_user_customer_scope VALUES
(32001,20001,10003,30001,'DOCTOR_ASSIGNED',10002,@now,10002,@now,0,0),
(32002,20001,10004,30001,'MANAGER_ASSIGNED',10002,@now,10002,@now,0,0);

INSERT INTO indicator_dict VALUES
(40001,20001,'fasting_glucose','空腹血糖','mmol/L','GLUCOSE',1,10001,@now,10001,@now,0,0),
(40002,20001,'fasting_insulin','空腹胰岛素','μIU/mL','GLUCOSE',1,10001,@now,10001,@now,0,0),
(40003,20001,'hba1c','糖化血红蛋白','%','GLUCOSE',1,10001,@now,10001,@now,0,0),
(40004,20001,'triglyceride','甘油三酯','mmol/L','LIPID',1,10001,@now,10001,@now,0,0),
(40005,20001,'hdl','高密度脂蛋白','mmol/L','LIPID',1,10001,@now,10001,@now,0,0),
(40006,20001,'crp','C反应蛋白','mg/L','INFLAMMATION',1,10001,@now,10001,@now,0,0),
(40007,20001,'tsh','促甲状腺激素','mIU/L','THYROID',1,10001,@now,10001,@now,0,0),
(40008,20001,'ft4','游离甲状腺素','pmol/L','THYROID',1,10001,@now,10001,@now,0,0);

INSERT INTO lab_report VALUES
(50001,20001,30001,'开发测试生化检验报告','2026-07-01','PUBLISHED','DEVELOPMENT_SEED',NULL,NULL,10004,@now,10003,@now,0,0);
INSERT INTO indicator_value VALUES
(51001,20001,50001,30001,'fasting_glucose','空腹血糖',6.200000,'mmol/L',3.900000,6.100000,'HIGH',1,10004,@now,10004,@now,0,0),
(51002,20001,50001,30001,'fasting_insulin','空腹胰岛素',12.500000,'μIU/mL',2.000000,25.000000,'NORMAL',1,10004,@now,10004,@now,0,0),
(51003,20001,50001,30001,'hba1c','糖化血红蛋白',5.800000,'%',4.000000,6.000000,'NORMAL',1,10004,@now,10004,@now,0,0),
(51004,20001,50001,30001,'triglyceride','甘油三酯',1.900000,'mmol/L',0.000000,1.700000,'HIGH',1,10004,@now,10004,@now,0,0),
(51005,20001,50001,30001,'hdl','高密度脂蛋白',1.200000,'mmol/L',1.000000,NULL,'NORMAL',1,10004,@now,10004,@now,0,0),
(51006,20001,50001,30001,'crp','C反应蛋白',4.100000,'mg/L',NULL,3.000000,'HIGH',1,10004,@now,10004,@now,0,0),
(51007,20001,50001,30001,'tsh','促甲状腺激素',2.600000,'mIU/L',0.270000,4.200000,'NORMAL',1,10004,@now,10004,@now,0,0),
(51008,20001,50001,30001,'ft4','游离甲状腺素',15.200000,'pmol/L',12.000000,22.000000,'NORMAL',1,10004,@now,10004,@now,0,0);

INSERT INTO ai_task VALUES
(52001,20001,50001,30001,'TASK_DEMO_001','HEALTH_ASSESSMENT','SUCCESS',NULL,@now,@now,10004,@now,10004,@now,0,0);
INSERT INTO health_assessment VALUES
(53001,20001,52001,50001,30001,'DEMO_1.0.0','SUCCESS','ATTENTION',JSON_OBJECT('taskId','TASK_DEMO_001','status','SUCCESS','results',JSON_ARRAY(JSON_OBJECT('modelCode','GLUCOSE_METABOLISM','modelName','糖代谢评估','score',68,'riskLevel','ATTENTION','evidence',JSON_ARRAY('示例：空腹血糖高于演示关注阈值'),'missingIndicators',JSON_ARRAY(),'recommendations',JSON_ARRAY('建议由专业人员审核')))),'该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。',10004,@now,10004,@now,0,0);
INSERT INTO assessment_review VALUES
(54001,20001,53001,30001,10003,'PUBLISHED','已核对演示指标，同意作为健康管理测试报告发布。',@now,10004,@now,10003,@now,0,0);
INSERT INTO health_report VALUES
(55001,20001,30001,53001,'HR_DEMO_001','测试客户的健康管理评估报告（演示）','PUBLISHED','开发测试报告：部分演示指标建议关注，需结合完整健康信息持续追踪。','已核对演示指标，同意作为健康管理测试报告发布。','该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。',@now,10003,10003,@now,10003,@now,0,0);
INSERT INTO followup_task VALUES
(56001,20001,30001,10004,'一周生活方式随访','记录睡眠、饮食与运动执行情况。',DATE_ADD(CURDATE(), INTERVAL 7 DAY),'PENDING',NULL,NULL,10003,@now,10004,@now,0,0);


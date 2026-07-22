-- 客户确认 OCR 数据，AI 初评仅供医生审核；健康管理师负责审核后的计划与随访。
-- 收回健康管理师的 OCR/AI 初评权限，收回医生的随访创建权限。
DELETE FROM sys_role_permission
WHERE tenant_id = 20001
  AND role_id = 11004
  AND permission_id IN (12004, 12005, 12006);

DELETE FROM sys_role_permission
WHERE tenant_id = 20001
  AND role_id = 11003
  AND permission_id = 12015;

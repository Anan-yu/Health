-- Follow-up execution belongs to health managers. Doctors review reports and risk conclusions.
DELETE urp
FROM sys_role_permission urp
JOIN sys_role r ON r.id = urp.role_id AND r.deleted = 0
JOIN sys_permission p ON p.id = urp.permission_id AND p.deleted = 0
WHERE r.role_code = 'DOCTOR' AND p.permission_code = 'followup:create';

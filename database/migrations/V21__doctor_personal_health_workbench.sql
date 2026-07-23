-- Doctors can use the same verified phone account as a health-service customer.
-- Keep DOCTOR as the default workbench; CUSTOMER is an optional personal health view.
SET @now = NOW();

-- V18 retired customer access for doctors by logically deleting the prior assignment.
-- Restore that exact row first, otherwise the unique user/role key would reject a duplicate row.
UPDATE sys_user_role customer_assignment
JOIN sys_user u ON u.id = customer_assignment.user_id AND u.deleted = 0
JOIN sys_role customer_role
  ON customer_role.id = customer_assignment.role_id
  AND customer_role.tenant_id = u.tenant_id
  AND customer_role.role_code = 'CUSTOMER'
  AND customer_role.deleted = 0
JOIN sys_user_role doctor_assignment
  ON doctor_assignment.user_id = u.id AND doctor_assignment.deleted = 0
JOIN sys_role doctor_role
  ON doctor_role.id = doctor_assignment.role_id AND doctor_role.role_code = 'DOCTOR' AND doctor_role.deleted = 0
SET customer_assignment.deleted = 0,
    customer_assignment.updated_by = 1,
    customer_assignment.updated_at = @now
WHERE customer_assignment.deleted = 1;

INSERT INTO sys_user_role
  (id, tenant_id, user_id, role_id, created_by, created_at, updated_by, updated_at, deleted, version)
SELECT UUID_SHORT(), u.tenant_id, u.id, customer_role.id, 1, @now, 1, @now, 0, 0
FROM sys_user u
JOIN sys_user_role doctor_assignment
  ON doctor_assignment.user_id = u.id AND doctor_assignment.deleted = 0
JOIN sys_role doctor_role
  ON doctor_role.id = doctor_assignment.role_id AND doctor_role.role_code = 'DOCTOR' AND doctor_role.deleted = 0
JOIN sys_role customer_role
  ON customer_role.tenant_id = u.tenant_id AND customer_role.role_code = 'CUSTOMER' AND customer_role.deleted = 0
LEFT JOIN sys_user_role customer_assignment
  ON customer_assignment.tenant_id = u.tenant_id
  AND customer_assignment.user_id = u.id
  AND customer_assignment.role_id = customer_role.id
WHERE u.deleted = 0 AND customer_assignment.id IS NULL;

-- The workbench table has the same unique key and V18 also logically deleted
-- the customer's historical workbench. Restore it before inserting missing rows.
UPDATE sys_user_workbench customer_workbench
JOIN sys_user u ON u.id = customer_workbench.user_id AND u.deleted = 0
JOIN sys_user_role doctor_assignment
  ON doctor_assignment.user_id = u.id AND doctor_assignment.deleted = 0
JOIN sys_role doctor_role
  ON doctor_role.id = doctor_assignment.role_id AND doctor_role.role_code = 'DOCTOR' AND doctor_role.deleted = 0
SET customer_workbench.deleted = 0,
    customer_workbench.is_default = 0,
    customer_workbench.updated_by = 1,
    customer_workbench.updated_at = @now
WHERE customer_workbench.workbench_code = 'CUSTOMER' AND customer_workbench.deleted = 1;

INSERT INTO sys_user_workbench
  (id, tenant_id, user_id, workbench_code, is_default, created_by, created_at, updated_by, updated_at, deleted, version)
SELECT UUID_SHORT(), u.tenant_id, u.id, 'CUSTOMER', 0, 1, @now, 1, @now, 0, 0
FROM sys_user u
JOIN sys_user_role doctor_assignment
  ON doctor_assignment.user_id = u.id AND doctor_assignment.deleted = 0
JOIN sys_role doctor_role
  ON doctor_role.id = doctor_assignment.role_id AND doctor_role.role_code = 'DOCTOR' AND doctor_role.deleted = 0
LEFT JOIN sys_user_workbench customer_workbench
  ON customer_workbench.tenant_id = u.tenant_id
  AND customer_workbench.user_id = u.id
  AND customer_workbench.workbench_code = 'CUSTOMER'
WHERE u.deleted = 0 AND customer_workbench.id IS NULL;

INSERT INTO health_patient
  (id, tenant_id, user_id, name, gender, birth_date, phone_masked, phone_hash, assigned_doctor_id,
   assigned_manager_id, status, created_by, created_at, updated_by, updated_at, deleted, version)
SELECT UUID_SHORT(), u.tenant_id, u.id, u.display_name, 'UNKNOWN', NULL, u.phone_masked, u.phone_hash, NULL,
       NULL, 'ACTIVE', 1, @now, 1, @now, 0, 0
FROM sys_user u
JOIN sys_user_role doctor_assignment
  ON doctor_assignment.user_id = u.id AND doctor_assignment.deleted = 0
JOIN sys_role doctor_role
  ON doctor_role.id = doctor_assignment.role_id AND doctor_role.role_code = 'DOCTOR' AND doctor_role.deleted = 0
LEFT JOIN health_patient personal_patient
  ON personal_patient.tenant_id = u.tenant_id AND personal_patient.user_id = u.id AND personal_patient.deleted = 0
WHERE u.deleted = 0 AND personal_patient.id IS NULL;

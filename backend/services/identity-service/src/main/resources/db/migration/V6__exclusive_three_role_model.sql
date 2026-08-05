-- Canonical product roles. Permission rows are synchronized by SystemAccessProfileBootstrap.
INSERT INTO roles(id, code, name, system_role, created_at, updated_at, version)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'ADMIN', 'Quản trị viên', true, now(), now(), 0),
  ('10000000-0000-0000-0000-000000000002', 'INSTRUCTOR', 'Giảng viên', true, now(), now(), 0),
  ('10000000-0000-0000-0000-000000000003', 'STUDENT', 'Học viên', true, now(), now(), 0)
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, system_role = true, updated_at = now();

CREATE TEMP TABLE canonical_user_role AS
SELECT u.id AS user_id,
       CASE
         WHEN u.account_type = 'SYSTEM_ADMIN'
              OR EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND upper(r.code) IN ('ADMIN','ACCESS_ADMINISTRATOR','ACCOUNT_MANAGER','ORGANIZATION_MANAGER','PLATFORM_CUSTOMIZER')
              ) THEN 'ADMIN'
         WHEN EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND upper(r.code) IN ('INSTRUCTOR','COURSE_AUTHOR','TRAINING_MANAGER','EXAM_MANAGER','GRADER')
              ) THEN 'INSTRUCTOR'
         ELSE 'STUDENT'
       END AS role_code
FROM user_accounts u;

DELETE FROM user_roles;
INSERT INTO user_roles(user_id, role_id)
SELECT c.user_id, r.id
FROM canonical_user_role c
JOIN roles r ON upper(r.code) = c.role_code;

-- Remove legacy ad-hoc permissions and scoped roles. Product permissions now come only
-- from the account's single canonical role, preventing cross-role capability leakage.
DELETE FROM authorization_grants;
DELETE FROM scoped_role_assignments;

-- Database-level guarantee: one account can own only one product role.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_single_product_role ON user_roles(user_id);

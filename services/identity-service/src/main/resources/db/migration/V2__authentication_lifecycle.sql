-- Preserve V1 and all existing accounts, profiles, addresses and assignments.
ALTER TABLE users ADD COLUMN full_name VARCHAR(200) NULL;
UPDATE users u LEFT JOIN profiles p ON p.user_id = u.id
SET u.full_name = LEFT(COALESCE(NULLIF(TRIM(CONCAT_WS(' ', p.first_name, p.last_name)), ''), u.email), 200);
ALTER TABLE users MODIFY full_name VARCHAR(200) NOT NULL;
UPDATE users SET status = 'INACTIVE' WHERE status = 'DISABLED';
UPDATE roles SET name = 'ROLE_USER' WHERE name = 'CUSTOMER';
UPDATE roles SET name = 'ROLE_ADMIN' WHERE name = 'ADMIN';
ALTER TABLE refresh_tokens ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN family_id VARCHAR(36) NULL;
-- Existing sessions are invalidated during this security upgrade.
UPDATE refresh_tokens SET revoked = TRUE, family_id = UUID();
ALTER TABLE refresh_tokens MODIFY family_id VARCHAR(36) NOT NULL;
CREATE INDEX idx_refresh_expiry ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_family ON refresh_tokens(family_id);
ALTER TABLE users ADD CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE','INACTIVE','LOCKED'));

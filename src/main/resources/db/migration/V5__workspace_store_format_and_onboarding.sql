-- V5: Add store format, location attributes, and persistent onboarding status to workspaces
ALTER TABLE workspaces ADD COLUMN store_format VARCHAR(50) DEFAULT 'SUPERMARKET';
ALTER TABLE workspaces ADD COLUMN city VARCHAR(100);
ALTER TABLE workspaces ADD COLUMN state VARCHAR(100);
ALTER TABLE workspaces ADD COLUMN country VARCHAR(100) DEFAULT 'India';
ALTER TABLE workspaces ADD COLUMN onboarding_status VARCHAR(50) DEFAULT 'COMPLETED';

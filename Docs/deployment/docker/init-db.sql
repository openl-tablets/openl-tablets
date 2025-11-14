-- OpenL Tablets Database Initialization Script
-- Creates separate databases and users for Studio and Rule Services
--
-- This script is automatically executed when PostgreSQL container starts
-- Place this file in docker-entrypoint-initdb.d/ directory

-- ==========================================
-- Create Databases
-- ==========================================

-- Database for OpenL Studio
CREATE DATABASE openl_studio;

-- Database for OpenL Rule Services
CREATE DATABASE openl_ruleservices;

-- ==========================================
-- Create Users
-- ==========================================

-- User for OpenL Studio
CREATE USER studio_user WITH PASSWORD 'studio_password';

-- User for OpenL Rule Services
CREATE USER rules_user WITH PASSWORD 'rules_password';

-- ==========================================
-- Grant Permissions
-- ==========================================

-- Grant all privileges on Studio database to studio_user
GRANT ALL PRIVILEGES ON DATABASE openl_studio TO studio_user;

-- Grant all privileges on Rule Services database to rules_user
GRANT ALL PRIVILEGES ON DATABASE openl_ruleservices TO rules_user;

-- ==========================================
-- Configure Default Privileges
-- ==========================================

-- Connect to Studio database and set up default privileges
\c openl_studio

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO studio_user;

-- Set default privileges for tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO studio_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO studio_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO studio_user;

-- Connect to Rule Services database and set up default privileges
\c openl_ruleservices

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO rules_user;

-- Set default privileges for tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO rules_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO rules_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO rules_user;

-- ==========================================
-- Create Extensions (if needed)
-- ==========================================

-- Extension for UUID support
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Extension for full-text search
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ==========================================
-- Performance Tuning (Optional)
-- ==========================================

-- Adjust these based on your workload
-- ALTER SYSTEM SET shared_buffers = '256MB';
-- ALTER SYSTEM SET effective_cache_size = '1GB';
-- ALTER SYSTEM SET maintenance_work_mem = '64MB';
-- ALTER SYSTEM SET checkpoint_completion_target = 0.9;
-- ALTER SYSTEM SET wal_buffers = '16MB';
-- ALTER SYSTEM SET default_statistics_target = 100;
-- ALTER SYSTEM SET random_page_cost = 1.1;
-- ALTER SYSTEM SET effective_io_concurrency = 200;
-- ALTER SYSTEM SET work_mem = '4MB';
-- ALTER SYSTEM SET min_wal_size = '1GB';
-- ALTER SYSTEM SET max_wal_size = '4GB';

-- ==========================================
-- Verification
-- ==========================================

-- Switch back to default database
\c postgres

-- List all databases
\l

-- List all users
\du

-- Success message
SELECT 'OpenL Tablets database initialization completed successfully!' AS message;

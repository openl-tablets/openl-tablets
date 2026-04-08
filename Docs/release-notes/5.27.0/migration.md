---
title: OpenL Tablets 5.27.0 Migration Notes
---

## Database Schema Updates

OpenL Tablets 5.27.0 includes updates to the user database schema to support new access control features.

### Pre-Upgrade Backup

A backup of your database is strongly recommended before upgrading to version 5.27.0. This ensures you can recover your data if any issues occur during the migration process.

```bash
# Example MySQL backup
mysqldump -u username -p database_name > backup_5.27.0.sql

# Example PostgreSQL backup
pg_dump -U username database_name > backup_5.27.0.sql
```

### Migration Impact

The schema changes are designed to be backward compatible with existing data. The upgrade process will automatically apply necessary schema migrations during startup.

### Post-Upgrade Verification

After upgrading, verify that:
- All users can log in successfully
- ACL permissions are functioning as expected
- Excel file merge operations work correctly
- Decision table expression references are properly resolved

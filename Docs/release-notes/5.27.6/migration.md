---
title: OpenL Tablets 5.27.6 Migration Notes
---

### Quick Role-Based Pointers

* **If you are an Administrator** → pay special attention to section **1**

---

### 1. PostgreSQL Schema Change

The behavior of OpenL Studio when configured to use PostgreSQL as the user database has changed. Previously, all tables were created in the `public` schema. Starting with 5.27.6, tables are created in the user's own schema.

To retain the previous behavior and keep tables in the public schema, execute the following command before upgrading:

```sql
ALTER ROLE my_studio_user_in_postgresql SET search_path TO public;
```

Replace `my_studio_user_in_postgresql` with your actual PostgreSQL role name.

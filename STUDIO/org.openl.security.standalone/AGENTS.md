# Security Standalone — Agent Instructions

Database-backed users, groups, tags and access control lists of OpenL Studio, plus the Liquibase change logs
that create and upgrade their schema.

## Database Change Logs

`resources/db/changelog/` holds one change log per release, listed in order by `db.changelog-master.xml`:

- **`db.changelog-6.0.0.xml`** — baseline: the schema, the default groups and the default permissions as
  OpenL Tablets 6.0.0 left them, created in one step on an empty database. The same file converts a database
  of 5.27.15 to that schema, so 5.27.15 is the oldest release an upgrade starts from.
- **`db.changelog-<release>.xml`** — the changes that release introduced.

`DBMigrationBean` applies them on startup. Liquibase records what it has applied in `DATABASECHANGELOG`.

The first change set, `6.0.0-supported-source-schema`, halts the migration on a database left behind by a
release older than 5.27.10: such a database still keeps its group hierarchy in `OpenL_Group2Group`, which
5.27.10 flattened and dropped, and the change set that flattens it is no longer here. The presence of that
table is what recognizes it. 5.27.15 is the oldest release the conversion is tested against.

## Strict Rules (**MUST**. No exceptions.)

- **Never edit a change set that has been released.** Liquibase stores a checksum per change set, and an edited
  one fails every upgrade. Ship the correction as a new change set instead.
- **Add a change to the change log of the release it ships in**, and register that file in
  `db.changelog-master.xml`. Create the file when the release has none yet.
- **Give every change set a precondition that marks it as run when its change is already there**
  (`onFail="MARK_RAN"`), so the change log also adopts a schema built by an earlier release. A precondition is
  answered before its change set runs, so it must not depend on what an earlier change set of the same run
  created.
- **Describe a change once**, and let Liquibase render it for each database. Use the properties of
  `db.changelog-master.xml` (`${varchar}`, `${timestamp}`, `${boolean}`, `${booleanTrue}`, `${booleanFalse}`)
  where a type or a literal differs per database, and `dbms` only where the statement itself differs.
- **Name every database a `dbms` attribute applies to**: `h2`, `postgresql`, `mysql`, `mariadb`, `mssql`,
  `oracle`. Azure SQL Database is served by `mssql`.
- **Keep raw `<sql>` for what has no change of its own** (a check constraint, seed data derived from generated
  keys). Prefer `<createTable>`, `<addColumn>`, `<addForeignKeyConstraint>` and the other changes over SQL.
- **Never quote an identifier.** The change logs set `objectQuotingStrategy="QUOTE_ONLY_RESERVED_WORDS"`,
  because a quoted name is an object an upgraded database does not have and the application cannot address:
  PostgreSQL would fold every mixed-case name differently, and Oracle quotes whatever Liquibase counts as
  reserved. A column whose name Liquibase escapes anyway, such as `password` on Oracle, is written in plain
  SQL.
- **Verify a change against every supported database** before committing: `mvn test -pl
  STUDIO/org.openl.security.standalone` covers H2 for every supported source release, and
  `mvn verify -pl ITEST/itest.studio/acl -am` upgrades a real database of 5.27.15 and of 6.0.0 on MySQL,
  SQL Server, Oracle and PostgreSQL.
- **Change logs are excluded from the formatter** of the root `pom.xml`, for the same reason they are never
  edited: reformatting changes the checksum Liquibase stores.

## Test Fixtures

`test-resources/db/openl-<release>-h2.sql` reproduces what each release left behind, layered on the one before
it: 5.27.15 creates the schema of that release, 6.0.0 converts it, 6.4.0 adds the last column. A test that
starts from a release runs the scripts up to it. When a release changes the schema, add the layer for it.

## Surrogate Keys of the ACL Tables

Spring Security reads the key of a freshly inserted `acl_sid` or `acl_class` row back with a query that differs
per database (`EnabledAclConfiguration`). H2 and Oracle are served by a named sequence, because neither reports
the value an identity column has just generated; the other databases are served by an identity column. Both
flavours are created by their own change set, and a new ACL column has to be added to both.

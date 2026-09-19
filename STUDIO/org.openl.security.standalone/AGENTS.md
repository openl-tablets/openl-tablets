# Security Standalone — Agent Instructions

Database-backed users, groups, tags and access control lists of OpenL Studio, plus the Liquibase change logs
that create and upgrade their schema.

## Database Change Logs

`resources/db/changelog/` holds the change logs, listed in order by `db.changelog-master.xml`:

- **`install/db.changelog-<area>.xml`** — the tables of one area as the current release has them, created in
  one step: `users`, `tags`, `lock`, `pat-tokens` and `acl`. Each change set is marked as run wherever its
  tables are already there, so the same file creates a table a database of an earlier release never had, and
  leaves alone one it did have.
- **`upgrade/db.changelog-<release>.xml`** — what a database of an earlier release still needs, one file per
  release: `6.0.0` converts the permission model of 5.27.15, `6.4.0` adds the column that release introduced,
  `6.5.0` drops the history table of the previous migration tool.

`DBMigrationBean` applies them on startup. Liquibase records what it has applied in `DATABASECHANGELOG`.

The split decides where a change belongs: a **new table** is only an install file, because the `tableExists`
precondition creates it on an old database as readily as on an empty one; a **change to a table an earlier
release already created** is both — the install file, so a new installation gets it, and the release's upgrade
file, so an old one does.

A change set that finds its change already there is marked as run, so an upgraded database keeps the column
types the release that created it chose: the ones a fresh install now declares `CHAR` or plain `VARCHAR` stay
`VARCHAR` or the national type there. Both spellings serve the application, and neither is converted.

The master change log halts the migration on a database left behind by a release older than 5.27.10: such a
database still keeps its group hierarchy in `OpenL_Group2Group`, which 5.27.10 flattened and dropped, and the
change set that flattens it is no longer here. The presence of that table is what recognizes it, through a
precondition of the change log itself, so nothing is applied and nothing is recorded. 5.27.15 is the oldest
release the conversion is tested against.

## Strict Rules (**MUST**. No exceptions.)

- **Never edit a change set of `upgrade/`.** Liquibase stores a checksum per change set, and an edited one
  fails every upgrade. Ship the correction as a new change set instead. An `install/` change set is the
  exception: it describes the tables of the current release, it can only ever run on a database that has none
  of them, and its `<validCheckSum>1:any</validCheckSum>` accepts the checksum of an edit — so a new change
  set carries that element too.
- **Add a new table to the `install/` file of its area**, and a change to an existing table to both that file
  and `upgrade/db.changelog-<release>.xml`. Create the upgrade file when the release has none yet, and
  register every new file in `db.changelog-master.xml`.
- **Give every change set a precondition that marks it as run when its change is already there**
  (`onFail="MARK_RAN"`), so the change log also adopts a schema built by an earlier release. A precondition is
  answered before its change set runs, so it must not depend on what an earlier change set of the same run
  created.
- **Describe a change once**, and let Liquibase render it for each database. Name the type Liquibase knows
  (`VARCHAR`, `CHAR`, `BIGINT`, `BOOLEAN`, `DATETIME`) and let it translate; reach for the properties of
  `db.changelog-master.xml` (`${varchar}`, `${booleanFalse}`, `${aclClassLength}`, `${aclIdentityLength}`)
  only where the translation is not the right one, and for `dbms` only where the statement itself differs.
- **Declare `${varchar}` only where national characters can appear** — text a person typed or an identity
  provider supplied. A column the product fills itself (a hash, a UUID, a Java class name, a setting key and
  its value) is `VARCHAR`, or `CHAR` when every value has the same length, which halves what SQL Server and
  Oracle store for it. A column a foreign key covers keeps the type of the column it references, because
  Oracle refuses a foreign key between a national and a non-national column: every `loginName` is
  `${varchar}`, whatever it holds. `${varchar}` is Liquibase's own `NVARCHAR`, which it translates per
  database — except on MySQL and MariaDB, where that spelling means `NATIONAL VARCHAR` and would pin the
  column to the three-byte `utf8mb3` instead of the character set of the database. Those two keep the plain
  `VARCHAR` line of their own; never collapse the pair into one.
- **Name every database a `dbms` attribute applies to**: `h2`, `postgresql`, `mysql`, `mariadb`, `mssql`,
  `oracle`. Azure SQL Database is served by `mssql`.
- **Keep raw `<sql>` for what has no change of its own** — a check constraint, a table whose column types come
  from the query that fills it, and above all an insert of the rows a query returns, since `<insert>` inserts
  only the rows it is given. Everything else is a change: `<createTable>`, `<addColumn>`,
  `<addForeignKeyConstraint>`, and `<insert>`, `<update>` and `<delete>` for data. A `CASE` over a handful of
  values is several `<update>` changes, one per value, ordered so that no value a later one looks for is
  produced by an earlier one.
- **Never quote an identifier.** Every change log sets `objectQuotingStrategy="QUOTE_ONLY_RESERVED_WORDS"` on
  its root element — an included file does not inherit it from the master, and without it PostgreSQL creates
  `"OpenL_Tag_Types"`, an object an upgraded database does not have and the application cannot address. Oracle
  likewise quotes whatever Liquibase counts as reserved. A column whose name Liquibase escapes anyway, such as
  `password` on Oracle, is written in plain SQL.
- **Verify a change against every supported database** before committing: `mvn test -pl
  STUDIO/org.openl.security.standalone` covers H2 for every supported source release, and
  `mvn verify -pl ITEST/itest.studio/acl -am` creates the schema on an empty MySQL, SQL Server, Oracle and
  PostgreSQL, and upgrades a real database of 5.27.15 and of 6.0.0 on each of them.
- **Change logs are excluded from the formatter** of the root `pom.xml`, for the same reason they are never
  edited: reformatting changes the checksum Liquibase stores.

## Every Table Has a Reader

A table belongs in the change log only while something reads or writes it. Two of them do not, and are
deliberately absent: `OpenL_Projects` and `OpenL_Project_Tags` held the tags a release before 6.0.0 attached
to a project, and `ProjectTagsMigrator` empties and drops them at the first startup after an upgrade. The
change log neither creates them nor drops them — a database upgraded from an older release brings them along,
and one that never had them does not need them.

Two readers are easy to miss when checking the rest. The ACL tables belong to Spring Security
(`EnabledAclConfiguration`, active unless `user.mode` is `single`), and `OPENL_LOCK` belongs to the lock
registry of Spring Integration, which names it from the `OPENL_` prefix of `lockRepository` rather than
spelling it out, so a search for that table name finds nothing.

## Test Fixtures

`test-resources/db/openl-<release>-h2.sql` reproduces what each release left behind, layered on the one before
it: 5.27.15 creates the schema of that release, 6.0.0 converts it, 6.4.0 adds the last column. A test that
starts from a release runs the scripts up to it. When a release changes the schema, add the layer for it.

## Surrogate Keys of the ACL Tables

Spring Security reads the key of a freshly inserted `acl_sid` or `acl_class` row back with a query that differs
per database (`EnabledAclConfiguration`). H2 and Oracle are served by a named sequence, because neither reports
the value an identity column has just generated; the other databases are served by an identity column. Both
flavours are created by their own change set, and a new ACL column has to be added to both.

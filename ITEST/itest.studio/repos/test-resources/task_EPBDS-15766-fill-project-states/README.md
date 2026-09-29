# Fill Tags for Project in every project state

Six projects match the `%FillState%-EPBDS-15766` template. The fill must write only the three it can change now:

- **Closed** — closed in the Git flat repository: the tags file is committed on behalf of the admin, the
  project stays closed and unlocked.
- **Opened** — opened by the admin: the tags file goes to the working copy, the project turns `EDITING`, and
  no commit is made.
- **Revision** — an older revision opened by the admin to be read: greyed out and left alone as `olderRevision`.
- **Shared** — opened by a second user who has not edited it: the tags are committed, the second user now
  reads an older revision, and their later save merges with the tags instead of dropping them.
- **Locked** — the second user is editing it: greyed out and left alone as `locked`, naming the second user.
- **Archive** — closed in the JDBC repository, which keeps projects as archives: greyed out and left alone as
  `archive`.

The preview and the fill give the same reason for a project that is left alone, and no tag value is created
for it.

# File repository without comment templates

The `design-file` repository uses `repo-file` without references to the default comment and new-branch settings.
The create, open, edit, save, copy, and delete steps exercise this configuration without supplying comments.

The expected behavior is that creating, copying, saving, and deleting projects works without comment or branch templates.
The `GET /rest/repos` response reports `comments: false` for `design-file`, and project details carry the same flag.
Project listings omit `canViewHistory` because `repo-file` retains only the current project state.

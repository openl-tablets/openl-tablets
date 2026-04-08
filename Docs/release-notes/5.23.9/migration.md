---
title: OpenL Tablets 5.23.9 Migration Notes
---

### Quick Role-Based Pointers

* **If you are an Administrator / Platform Owner** → pay special attention to section **1**

---

### 1. Repository Configuration Changes

Repository factory class names have been replaced with short aliases in repository configuration. Update any custom
repository configurations accordingly:

| Old Value (Full Class Name)                            | New Value (Alias) |
|:-------------------------------------------------------|:------------------|
| `org.openl.rules.repository.git.GitRepository`         | `repo-git`        |
| `org.openl.rules.repository.jcr.JcrRepository`         | `repo-jcr`        |
| `org.openl.rules.repository.db.DBRepository`           | `repo-jdbc`       |
| `org.openl.rules.repository.file.FileSystemRepository` | `repo-file`       |

Branch name pattern placeholders have also changed. Update any configured branch name patterns:

| Old Placeholder | New Placeholder  |
|:----------------|:-----------------|
| `{0}`           | `{project-name}` |
| `{1}`           | `{username}`     |

# Ignored and protected paths

The files of a project are kept apart from its rules by two settings:

- **Ignored paths**, such as `.git/`, `.github/` or the build output `target/`: a project created from an archive
  leaves them out, the Files API of a project and the Repository Files API refuse to write them and do not find them,
  and an archive uploaded through either API leaves them out and writes the rest.
- **Protected files** of a project, such as its root `pom.xml`: a project created from an archive leaves them out,
  but a write of one file writes them; the Files API of a project lists them as hidden, an upload into the project
  leaves the root one out, and an upload that replaces the project keeps the one the project holds. A `pom.xml` deeper
  in a project, or in the Repository Files API, is an ordinary file.

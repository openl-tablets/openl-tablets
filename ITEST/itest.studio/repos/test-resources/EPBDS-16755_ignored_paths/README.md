# Ignored paths

The build, VCS and CI files of a project, such as `pom.xml`, `.git/` or `.github/`, are kept out of reach by the
**Ignored paths** setting:

- a project created from an archive leaves them out;
- the Files API of a project and the Repository Files API refuse to write them and do not find them;
- an archive uploaded through either API leaves them out and writes the rest.

## Pre-check

1. Ensure all required tickets and issues are resolved, all required branches and commits are merged, and tests are updated.
2. Run the [Build](https://github.com/openl-tablets/openl-tablets/actions/workflows/build.yml) action.
   It must be green.
3. Run the [Deploy Maven artifacts](https://github.com/openl-tablets/openl-tablets/actions/workflows/deploy.yml) action.
   This action uploads DEMO, WebStudio and RuleServices artifacts to [GitHub packages](https://github.com/orgs/openl-tablets/packages?repo_name=openl-tablets).
4. Execute all automation tests.
5. Execute all performance tests.
6. Execute all backward compatibility tests.
7. Execute all supported platform and environment compatibility tests.

## Release Process

1. Run the [Release OpenL Tablets](https://github.com/openl-tablets/openl-tablets/actions/workflows/release.yml) action.
2. Log in [SonaType](https://oss.sonatype.org/), verify staged artifacts, and click the 'Release' button.
**This step is irreversable, so be careful with what is released.**
   Artifacts become available in [Central Maven Repository](https://repo1.maven.org/maven2/org/openl/) within a few hours.
3. After the release artifacts appear in [Central Maven Repository](https://repo1.maven.org/maven2/org/openl/),
run the [DockerHub Publisher](https://github.com/openl-tablets/openl-tablets/actions/workflows/docker.yml) action.
4. For each product, update the supported tags on the [DockerHub](https://hub.docker.com/u/openltablets) page.
5. Go to the [Releases](https://github.com/openl-tablets/openl-tablets/releases) section and publish the release artifacts on GitHub.
6. If necessary, update the DEMO download to the latest version at [Sourceforge](https://sourceforge.net/projects/openl-tablets/files/).
7. Publish the release and migration notes at https://openl-tablets.org/.
8. Do [tweet](https://twitter.com/openltablets) and email to the OpenL Tablets Announcements group.

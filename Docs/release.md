## Pre-check

1. Ensure all required tickets/issues were resolved. All required branches/commits were merged. Tests were updated.
2. Run [Build](https://github.com/openl-tablets/openl-tablets/actions/workflows/build.yml) action. It must be green.
3. Run [Deploy Maven artifacts](https://github.com/openl-tablets/openl-tablets/actions/workflows/deploy.yml) action.
4. Execute all automation tests.
5. Execute all performance tests.
6. Execute all backward compatibility tests.
7. Execute all supported platform and environment compatibility tests.

## Release process

1. Run [Release OpenL Tablets](https://github.com/openl-tablets/openl-tablets/actions/workflows/release.yml) action
2. Log in to [SonaType](https://oss.sonatype.org/) site and verify staged artifacts. Press 'Release' button.
**This step is irreversable, so be careful what are You releasing.**
Artifacts will be available in the [Central Maven Repository](https://repo1.maven.org/maven2/org/openl/) in a few hours.
3. After the release artifacts are appeared in the [Central Maven Repository](https://repo1.maven.org/maven2/org/openl/),
run [DockerHub Publisher](https://github.com/openl-tablets/openl-tablets/actions/workflows/docker.yml) action.
4. Update supported tags on the [DockerHub](https://hub.docker.com/u/openltablets) page for each product. 
5. Go to the [Releases](https://github.com/openl-tablets/openl-tablets/releases) section and publish the release artifacts on the GitHub.
6. Update the DEMO download to the latest version on [Sourceforge](https://sourceforge.net/projects/openl-tablets/files/) if it is necessary.
7. Publish release and migration notes on https://openl-tablets.org/ site.
8. Do [tweet](https://twitter.com/openltablets) and email to the OpenL Announcements group.

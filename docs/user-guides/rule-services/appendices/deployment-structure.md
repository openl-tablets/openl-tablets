## Appendix G: Deployment Project ZIP Structure

OpenL projects without Excel files inside are supported

Deployment projects described in this section can be built via **OpenL Maven Plugin** or archived manually. The following topics are included:

-   [Single Project Deployment Structure](#single-project-deployment-structure)
-   [Multiple Projects Deployment Structure](#multiple-projects-deployment-structure)

### Single Project Deployment Structure

Deployable single project must be archived into ZIP file and have the following structure:

```
deployment.zip:
    rules.xml                    OpenL Tablets project descriptor
    rules-deploy.xml             OpenL Tablets project deployment configuration
    *.xlsx                       Excel files with rules
```

OpenL Tablets project descriptor and project deployment configuration are optional and can be skipped in deployment archive.

### Multiple Projects Deployment Structure

Deployable multiple projects must be archived into ZIP file and have the following structure:

```
deployment.zip:
        deployment.yaml                        OpenL Tablets deployment descriptor
        project-1                                OpenL Tablets project folder #1
                rules.xml
                rules-deploy.xml
                *.xlsx
        project-2                                OpenL Tablets project folder #2
rules.xml
                rules-deploy.xml
                *.xlsx
        project-*                                OpenL Tablets project folder #N
rules.xml
                rules-deploy.xml
                *.xlsx
```

This type of deployment is useful when several projects have mutual dependencies and must be deployed as single deployment.

OpenL Tablets deployment descriptor is a marker which tells OpenL Tablets Engine that this type of deployment may contain several OpenL Tablets projects. This file is mandatory and may optionally contain the name property to customize deployment name:

```yaml
name: openl-multiple-project-deployment
```


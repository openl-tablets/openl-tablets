## Appendix A: ZIP Project Structure

ZIP projects described in this section can be imported into OpenL Studio. The following topics are included:

-   [Single Project Structure](#single-project-structure)
-   [Single Project Structure \#2](#single-project-structure-2)

### Single Project Structure

A single project must be archived into ZIP file and have the following structure:

```
my-project.zip:
    rules.xml          			    OpenL Tablets project descriptor
    rules-deploy.xml		       OpenL Tablets project deployment configuration
    *.xlsx				                 Excel files with rules
```

OpenL Tablets project descriptor and project deployment configuration are optional and can be skipped in a single project structure.

### Single Project Structure \#2

For a special case when an archive contains a single folder in the root, use the following structure:

```
my-project.zip:
    my-			project              Folder with OpenL Tablets project inside
        rules.xml
        rules-deploy.xml
        *.xlsx
```

This type of archive is supported by OpenL Studio only.


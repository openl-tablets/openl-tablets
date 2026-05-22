# OpenL Tablets — Rules Project Structure

- `rules.xml` - the project descriptor and is **required** for every OpenL rules project
- `rules-deploy.xml` - the deployment descriptor
- `pom.xml` - the Maven project file, required only for projects built with Maven
- `rules/` - the conventional folder for OpenL tables
    - `2025-01-01_US/` - versioned folder by properties-file-name-pattern in `rules.xml`
        - `FooRules.xlsx` - rules for US market effective 2025-01-01 in Excel file
    - `PricingRules_US.xlsx` - rules for US market with no effective date (always active)
    - `SharedRules.xlsx` - rules shared across markets, effective date and properties-file-name-pattern agnostic
- `i18n/` is the conventional folder for localization property files, if used via msg() function
    - `message.properties` - messages for the ROOT locale (fallback)
    - `message_en_US.properties` - messages for English (United States)
- `groovy/` - Groovy scripts accessible from rule tables
    - `spring/` - Spring Framework related scripts
        - `SpringConfig.groovy` - the Spring configuration entry point
    - `Service.groovy` - service interface which defined in `rules-deploy.xml` as `serviceClass`
- `tests/` - for Test, Data and Run tables
- `tags.properties` - tag definitions of this project
- `openapi.json` - OpenAPI specification file for reconciliation or rules generation
- `kafka-deploy.yaml` - Deployment configuration for the Kafka Publisher.
- `README.md` - project documentation and instructions for developers
- `AGENTS.md` - guidelines for contributors working on this project
- `lib/` - for JAR dependencies available to all modules at runtime
    - `some-library.jar`

> [Note:]
> OpenL rules files may use obsolete `.xls` binary format, but `.xlsx` is recommended for better performance and
> compatibility with modern Excel features.

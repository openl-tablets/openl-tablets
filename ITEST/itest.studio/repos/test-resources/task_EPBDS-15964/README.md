# EPBDS-15964 — missing classes in `rules-deploy.xml`

Every class configured by `rootClassNamesBinding` is required project configuration. If one is absent from the
project classpath, OpenL Studio refuses to start execution and identifies the class that must be corrected or added.

- `010-setup` — creates an opened project and adds a `rules-deploy.xml` that names a missing project class.
- `020-missing-deployment-class` — a missing `rootClassNamesBinding` class produces 409 with its name, and the
  summary endpoint confirms that no test execution task was registered.
- `030-missing-property-naming-strategy` — the same contract applies when `jackson.propertyNamingStrategy` wraps the
  missing-class failure in a mapper-configuration error.
- `999-tierdown` — closes the project without saving the invalid configuration and deletes it.

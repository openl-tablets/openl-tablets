# Local Project Dependencies with Logical Names

The pre-seeded local workspace contains two project folders whose names differ from the logical names declared in
their `rules.xml` files. The source declares the target by its logical name.

The scenario verifies that OpenL Studio resolves the dependency, returns a usable target project id, and does not
advertise the Design repository Save action for either local-only project.

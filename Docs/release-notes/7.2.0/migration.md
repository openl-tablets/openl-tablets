---
title: "OpenL Tablets 7.2.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.2.0 requires no database changes and no Java version change. The table properties that
rules read through `$properties` and `$dispatchingProperties` have an interface type now, which affects the Java code
that uses that type, as listed for the developers.

## Developers

* **`org.openl.rules.binding.TableProperties` is an interface.** `$properties` and `$dispatchingProperties` return
  objects of `TablePropertiesImpl`, which implements it, so that a Test table can build the properties it expects. The
  getters are the same, and so is the JSON of a service that returns the properties, unless the
  `jackson.defaultTypingMode` of the service writes the type of such a value: the type is
  `org.openl.rules.binding.TablePropertiesImpl` then. Recompile the Java code that uses the type, such as a client of
  the service. Code that created it with `new TableProperties(properties)` no longer compiles: the properties come
  from rules only.

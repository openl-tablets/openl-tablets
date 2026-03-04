---
title: OpenL Tablets 5.9.1 Migration Notes
---

## Web Services Migration Steps

1. Update the OpenL version from 5.9.0 to 5.9.1.

2. Update `WEB-INF/openl-ruleservice-beans.xml`:
    * Replace `RuleService` bean with `RuleServiceImpl`.
    * Replace `ServiceManager` bean with `ServiceManagerImpl`.

3. Update `rating-server-core-loader-beans.xml`:
    * Change the `RulesLoader` bean to `RuleServiceLoaderImpl`.

4. Add the following property to `ruleservice.properties`:
   ```
   ruleservice.logging.enabled = true
   ```

5. Update `rating-server-core-publisher-beans.xml`:
    * Remove the `deploymentAdmin` bean.
    * Add `instantiationStrategyFactory` and `ruleServiceInstantiationFactory` beans.
    * Replace the `rulesPublisher` bean with `ruleServicePublisher`.
    * Update `loggingFeature` to use the custom implementation.

6. API changes: Review class renames and method return type modifications.

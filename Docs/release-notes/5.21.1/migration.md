---
title: OpenL Tablets 5.21.1 Migration Notes
---

## Rule Services

* The deprecated class `org.openl.rules.ruleservice.publish.WebServicesRuleServicePublisher` has been removed. Update
  any custom configurations that reference it.
* The CXF library now uses the slf4j adapter instead of direct log4j usage. Review logging configurations accordingly.
* JSTL and `spring-webmvc` libraries have been removed from Rule Service.

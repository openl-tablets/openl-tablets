package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Map;

record MethodDescription(String description, Map<String, String> parameterDescriptions) {
}

package org.openl.rules.ruleservice.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath*:META-INF/openl/extension-*.xml")
public class ExtensionsConfiguration {
}

## Customization of the CXF REST endpoints

OpenL Tablets Rule Services web service allows to customize a CXF Server by adding features which are initialized by
the Spring Framework.

### Enable logging feature

```java
package org.openl.rules.ruleservice.spring;

import java.util.Set;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {

    @Bean
    LoggingFeature loggingFeature() {
        var loggingFeature = new LoggingFeature();
        loggingFeature.setSensitiveProtocolHeaderNames(Set.of("Authorization"));
        loggingFeature.addBinaryContentMediaTypes("application/zip");
        return loggingFeature;
    }
}
```

## Adding Spring Framework configurations

OpenL Rule Services imports all Spring Framework configurations which are exist in
`org.openl.rules.ruleservice.spring` Java package or are matched to 
`classpath*:META-INF/openl/extension-*.xml` pattern.

So it is possible to register Spring beans in Java, like:
```java
package org.openl.rules.ruleservice.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfiguration {

    @Bean
    String clientId() {
        return "ABCDEF1234";
    }
}
```
or in `META-INF/openl/extension-my-configuration.xml` XML configuration file:
```xml
<?xml version='1.0'?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="clientId" class="java.lang.String">
        <constructor-arg value="ABCDEF1234"/>
    </bean>
</beans>
```

Note: The location of the `META-INF/openl/extension-*.xml` files is different for the Web application.
It should be located in `WEB-INF/classes/META-INF/openl/extension-my-configuration.xml`

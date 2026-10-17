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

> [!Note]
> A file added to the web application directly, not inside a JAR file, goes to
> `WEB-INF/classes/META-INF/openl/extension-my-configuration.xml`.

## Spring configuration of a rules project

A rules project can have a Spring configuration of its own: a `spring.SpringConfig` class annotated with
`@Configuration`, usually the `groovy/spring/SpringConfig.groovy` script. OpenL Rule Services creates a Spring context
for the service of the project from it and creates the service interceptors in that context.

```groovy
package spring

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class SpringConfig {
}
```

The configuration can proxy beans by class, as `@EnableAspectJAutoProxy(proxyTargetClass = true)` does, without
starting the JVM with `--add-opens=java.base/java.lang=ALL-UNNAMED`. A proxy intercepts every method of the bean that is
not private, static or final.

The exception is a class whose package does not accept the proxy: a JDK class, such as `java.util.HashMap`, or a class
of a `java.*` or `javax.*` package. Its proxy intercepts only public and protected methods. A call to a package-private
method is not passed to the bean: it runs on the proxy, whose fields are not set. A non-public JDK class cannot be
proxied by class.

Services that proxy the same class of the application, such as a class of a library, share one proxy class when they
proxy it the same way. A service that proxies it otherwise, with other advice or interfaces, gets a proxy of its own,
which intercepts only public and protected methods.

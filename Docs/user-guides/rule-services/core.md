## Rule Services Core

This section introduces Rule Services Core functionality and includes the following topics:

-   [Adding Dependencies into the Project](#adding-dependencies-into-the-project)
-   [Configuring Spring Integration for Rule Services Core](#configuring-spring-integration-for-rule-services-core)
-   [Customizing and Configuring Rule Services Core](#customizing-and-configuring-rule-services-core)

### Adding Dependencies into the Project

To use the Rule Services Core within Maven, declare the module dependencies in the project object model (POM) as described in the following example:

```xml
<dependency>
    <groupId>org.openl.rules</groupId>
    <artifactId>org.openl.rules.ruleservice</artifactId>
    <version>${openl.version}</version>
</dependency>
```

If Apache Maven is not used in the project, it is recommended to download all dependencies via Maven and add all downloaded dependencies into the existing project classpath.

### Configuring Spring Integration for Rule Services Core

This section describes how to configure Spring and Rule Services Core integration and includes the following topics:

-   [Adding a Bean Configuration File to the Spring Context Definition](#adding-a-bean-configuration-file-to-the-spring-context-definition)
-   [Simple Java Frontend Implementation](#simple-java-frontend-implementation)

#### Adding a Bean Configuration File to the Spring Context Definition

To support the Rule Services Core features, add the `openl-ruleservice-beans.xml` bean configuration file into the application Spring context definition. An example is as follows:

`<import resource="classpath:openl-ruleservice-beans.xml" />`

After adding the Rule Services Core beans, Spring configuration has a simple Java frontend service as a default publisher for all OpenL Tablets services.

#### Simple Java Frontend Implementation

Spring configuration defined in the `openl-ruleservice-beans.xml` file registers the `frontend` bean with default frontend implementation. This bean implements the `org.openl.rules.ruleservice.simple.RulesFrontend `interface that is designed to interact with deployed OpenL Tablets services.

| Inceptor                                                                                            | Description                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `OpenLService findServiceByName(String serviceName)`                                                | Find registered OpenL Tablets service by name.                                                                                                    |
| `Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params)` | Invokes a rule with the defined parameter types and parameter values from the deployed OpenL Tablets service.                                     |
| `Object execute(String serviceName, String ruleName, Object... params)`                             | Invokes a rule with the defined parameter values from the deployed OpenL service. <br/>Parameter types are automatically defined from sent parameters. |
| `Object getValue(String serviceName, String fieldName)`                                             | Returns field value from the defined OpenL Tablets service.                                                                                       |
| `Collection<String> getServiceNames()`                                                              | Returns a list of registered OpenL Tablets services.                                                                                              |
| `void registerService(OpenLService service)`                                                        | Registers the OpenL Tablets service.                                                                                                              |
| `void unregisterService(String serviceName)`                                                        | Unregisters the OpenL Tablets service.                                                                                                            |
| `<T> T buildServiceProxy(String serviceName, Class<T> proxyInterface)`                              | Builds a proxy for the OpenL Tablets service with a defined interface.                                                                            |
| `<T> T buildServiceProxy(String serviceName, Class<T> proxyInterface, ClassLoader classLoader)`      | Builds a proxy for the OpenL Tablets service with a defined interface and defined class loader.                                                   |

The `frontend `bean can be injected to user’s bean to interact with deployed OpenL Tablets services.

`OpenLServiceFactoryBean` is a factory bean implementation used to create a proxy object to interact with OpenL Tablets service. To create a proxy object, define a been factory as described in the following example:

```xml
<bean id="service1" class="org.openl.rules.ruleservice.simple.OpenLServiceFactoryBean">
    <!-- <property name="rulesFrontend" ref="frontend"/> optional. For custom implementation of RulesFrontend  -->
    <property name="serviceName" value="service1"/>
    <property name="proxyInterface" value="com.myproject.Service1"/>
</bean>
```

In this example, `serviceName` is a name of the deployed OpenL Tablets service and `proxyInterface` is an interface for building a proxy object. All invocations of proxy object methods are delegated to the `execute `method of the `frontend `bean. The invoked method name with its parameters is used as input parameters for the `execute `method.

**Note:** Proxy beans and proxy objects created by `frontend` bean are automatically updated if the OpenL Tablets service is redeployed into a data source. Nevertheless, these objects are not working while the project is redeployed. To synchronize this process, use Service Publisher listeners described in further sections.

### Customizing and Configuring Rule Services Core

The Rule Services Core module configuration features resemble configuration features for OpenL Rule Services. The OpenL Rule Services customization and configuration information is provided in this document and can be applied to Rule Services Core in the same way. For the list of components supported only by OpenL Rule Services, see diagrams in [Introduction](#introduction).


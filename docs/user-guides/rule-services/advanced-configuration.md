## OpenL Rule Services Advanced Configuration and Customization

This section describes OpenL Rule Services advanced services configuration and customization and explains the following:

-   [OpenL Rule Services Customization Algorithm](#openl-rule-services-customization-algorithm)
-   [Data Source Listeners](#data-source-listeners)
-   [Service Publishing Listeners](#service-publishing-listeners)
-   [Dynamic Interface Support](#dynamic-interface-support)
-   [Service Customization through Annotations](#service-customization-through-annotations)
-   [Customization of Log Requests to OpenL Rule Services and Their Responds in a Storage](#customization-of-log-requests-to-openl-rule-services-and-their-responds-in-a-storage)

### OpenL Rule Services Customization Algorithm

If a project has specific requirements, OpenL Rule Services customization algorithm is as follows:

1.  Create a Maven project that extends OpenL Rule Services.
2.  Add or change the required points of configuration.
3.  Add the following dependency to the `pom.xml` file with the version used in the project specified:
        
    ```xml
    <dependency>
        <groupId>org.openl.rules</groupId>
        <artifactId>org.openl.rules.ruleservice.ws</artifactId>
        <version>5.X.X</version>
        <type>war</type>
        <scope>runtime</scope>
    </dependency>
    ```
        
1.  Use the following Maven plugin to control the OpenL Rule Services building with user’s custom configurations and classes:
        
    ```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-war-plugin</artifactId>
        <configuration>
            <warSourceDirectory>webapps/ws</warSourceDirectory>
            <!—Define war name here-->
            <warName>${war.name}-${project.version}</warName>
            <packaging Excludes>
                <!—Exclude unnecessary libraries from parent project here-->
                WEB-INF/lib/org.openl.rules.ruleservice.ws.lib-*.jar
            </packaging Excludes>
            <!—Define paths for resources. Developer has to create a file with the same name to overload existing file in the parent project-->
            <web Resources>
                <resource>
                    <directory>src/main/resources</directory>
                </resource>
                <resource>
                    <directory>war-specific-conf</directory>
                </resource>
            </web Resources>
        </configuration>
    </plugin>
    ```
        
1.  If necessary, add customized spring beans into openl-ruleservice-override-beans.xml in src/main/resources.

### Data Source Listeners

A data source registers data source listeners and notifies some components of OpenL Rule Services about modifications. The only available event type on the production repository modification is about newly added deployment.

A service manager is always a data source listener because it must handle all modifications in the data source.

Users can add their own listener implementing `org.openl.rules.ruleservice.loader.DataSourceListener` for additional control of data source modifications with the required behavior and register it in data source via Spring configuration.

### Service Publishing Listeners

Service publishing listeners notify about the deployed or undeployed OpenL Tablets projects. Users can add their own listeners implementing `org.openl.rules.ruleservice.publisher.RuleServicePublisherListener` for additional control of deploying and undeploying projects with the required behavior and add them to the Spring configuration. The system automatically finds and registers all Spring beans implemented `RuleServicePublisherListener` interface as a publishing listener.

The `org.openl.rules.ruleservice.publisher.RuleServicePublisherListener` interface has the following methods:

| Inceptor                         | Description                                                                                               |
|----------------------------------|-----------------------------------------------------------------------------------------------------------|
| onDeploy(OpenLService)         | Invoked each time when the OpenL Tablets service is deployed with the publisher that fires this listener. |
| onUndeploy(String serviceName) | Invoked each time when the service with the defined name is undeployed.                                   |

### Dynamic Interface Support

OpenL Rule Services supports interface generation for services at runtime. This feature is called **Dynamic Interface Support.** If a static interface is not defined for a service, the system automatically generates an interface at runtime with all methods defined in the module or, in case of a multimodule, in the list of modules.

This feature is enabled by default. To use a dynamic interface, do not define a static interface for a service in `rules-deploy.xml `service description file.

It is not a good practice to use all methods from a module in a generated interface because of the following limitations:

-   All return types and method arguments in all methods must be transferrable through network.
-   An interface for web services must not contain the method designed for internal usage.

The system provides a mechanism for filtering methods in modules by including or excluding them from the dynamic interface.

This configuration can be applied to projects using the `rules.xml` file. An example is as follows:

```xml
<project>
    <name>project-name</name>
    <modules>
        <module>
            <name>module-name</name>
            <rules-root path="rules/Calculation.xlsx"/>
            <method-filter>
                <includes>
                    <value>.*determinePolicyPremium.*</value>
                    <value>.*vehiclePremiumCalculation.*</value>
                </includes>
           </method-filter>
       </module>
    </modules>
    <classpath>
        <entry path="lib/*"/>
    </classpath>        
</project>
```

For filtering methods, define the `method`-filter tag in the `rules.xml` file. This tag contains the `includes` and `excludes` tags. The algorithm is as follows:

-   If the `method`-filter tag is not defined in the `rules.xml`, the system generates a dynamic interface with all methods provided in the module or modules for multimodule.
-   If the `includes` tag is defined for method filtering, the system uses the methods which names match a regular expression of defined patterns.
-   If the `includes` tag is not defined, the system includes all methods.
-   If the `excludes` tag is defined for method filtering, the system uses methods which method names do not match a regular expression for defined patterns.
-   If the excludes tag is not defined, the system does not exclude the methods.

If OpenL Tablets Dynamic Interface feature is used, a client interface can also be generated dynamically at runtime. Apache CXF supports the dynamic client feature. For more information on dynamic interface support by Apache CXF, see <http://cxf.apache.org/docs/dynamic-clients.html>.

Note: If a project is empty and does not contain any method, it is unavailable as a service.

### Service Customization through Annotations

This section describes interface customization using annotations. The following topics are included:

-   [Interceptors for Methods](#interceptors-for-methods)
-   [Method Return Type Customization through Annotations](#method-return-type-customization-through-annotations)
-   [REST Endpoint Customization through Annotations](#rest-endpoint-customization-through-annotations)
-   [Customization through Annotations for Dynamic Generated Interfaces](#customization-through-annotations-for-dynamic-generated-interfaces)

#### Interceptors for Methods

Required Maven dependency for OpenL Rule Services annotations is org.openl.rules:org.openl.rules.ruleservice.annotation. Use the provided scope for dependency because this dependency already exists in OpenL Rule Services and it must not be included in the deployment distributive to avoid class duplication in the Java ClassLoader.

Interceptors for service methods can be specified using the following annotations:

-   `@org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor`

This annotation is used to define “before” interceptors for the annotated method. The goal of these interceptors is to add extra logic before service method invocation, such as validation for service method arguments, or to change values in input arguments. A class of the “before” interceptor must implement the `org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice` interface.

An example is as follows:

```java
public class RequestModelValidator implements ServiceMethodBeforeAdvice {
    public void before(Method interfaceMethod, Object proxy, 
                       Object... args) throws Throwable {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Service method should have at least one argument");
        }
        //other validation logic
    }
}
```

To use the “before” interceptor, proceed as follows:

```java
@ServiceMethodBeforeAdvice({ RequestModelValidator.class })
Result doSomething(RequestModel requestModel);
```

-   `@org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor`

This annotation is used to define “around” interceptors. A class for the “around” interceptor must implement the `org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice` interface. “Around” interceptors are used to add around logic for service method invocation. An example is when arguments of the case service method must be converted to another type before using them in service rules, and the results also require additional processing before return.

An example is as follows:

```java
public class MyMethodAroundInterceptor implements ServiceMethodAroundAdvice<Response> {
    @Override
    public Response around(Method interfaceMethod, Method proxyMethod, Object proxy, Object... args) throws Throwable {
        Result res = (Result) proxyMethod.invoke(proxy, args);
        return new Response("SUCCESS", res);
    }
}
```

To use the “around” interceptor, proceed as follows:

```java
@ServiceCallAroundInterceptor({ MyMethodAroundInterceptor.class })
Response doSomething(RequestModel requestModel);
```

-   `@org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor`

This annotation is used to defined “after” interceptors. This type of interceptions is used for result processing or error handling before return by the service method.

The following table describes “after” interceptor types:

| Inceptor          | Description                                                                                                                                                                                                                                                                                                                                                                                                          |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AfterReturning` | Intercepts the result of a successfully calculated method, with a possibility of post processing of the return result, including result conversion to another type. <br/>In this case, the type must be specified as the return type for the method in the service class. <Br/>`AfterReturning` interceptors must be a subclass of `org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice.` |
| `AfterThrowing`  | Intercepts a method that has an exception thrown, with a possibility of post processing of an error and throwing another type of exception. <br/>`AfterThrowing` interceptors must be a subclass of `org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterThrowingAdvice.`                                                                                                                           |

Example of the “after” interceptor implementation with after returning logic is as follows:

```java
public class SpreadsheetResultConverter extends
                               AbstractServiceMethodAfterReturningAdvice<ResponseDTO> {

    @Override
    public ResponseDTO afterReturning(Method interfaceMethod,
                                      Object result, Object... args) {
        SpreadsheetResult = (SpreadsheetResult) result;
        return mapSpreadsheetResultToResponseDTO(spreadsheetResult);
    }

    private ResponseDTO mapSpreadsheetResultToResponseDTO(SpreadsheetResult result) {
        ResponseDTO response = new ResponseDTO();
        response.setPremium((Double) result.getFieldValue("$Value$PremiumStep"));
        // Do some other mapping logic...
        return response;
    }
}
```

Example of the “after” interceptor implementation with after throwing logic is as follows:

```java
public class ExceptionHandlingAdvice extends AbstractServiceMethodAfterThrowingAdvice <ResponseDTO> {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);
    @Override
    public ResponseDTO afterThrowing(Method iMethod, Exception t, Object... args) {
        LOG.error(t.getMessage(), t);
        return new ResponseDTO("INTERNAL_ERROR", t.getMessage());
    }
}
```

To use the “after” interceptor, proceed as follows:

```java
@ServiceCallAfterInterceptor({ SpreadsheetResultConverter.class, ExceptionHandlingAdvice.class })
ResponseDTO doSometing(Request request);
```

Use `@org.openl.rules.ruleservice.core.interceptors.annotations.NotConvertor` or `@org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType` on an interceptor implementation class when an interceptor must return a type of the generated class that is not available at compilation time to use as a generic parameter of the interceptor class. The `NotConvertor` annotation instructs the system that the interceptor does not change the return type of the method even if `Object` or any other class is used as a generic parameter of the class. The `UseOpenMethodReturnType` annotation instructs the system that the interceptor returns the original type of the rules method even if any other type is used as a generic parameter of the interceptor class.

-   `@org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod`

This annotation is used to define the extra method absent in OpenL rules. Additional method implementation must implement `org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler` interface, and it exposes methods that differ in signature with the rules or do not exist in the Excel sheet.

For example, an Excel file contains the `String hello(String)` method and this method must be exposed as `String hello(Integer)`.

The advice class uses the same class loader that is used to compile the OpenL Tablets project. It means that a user can access all datatype classes generated by the system for a particular project. An additional method can be used when additional mapping between the OpenL Tablets model and external model is required, for example:

```java
    public static class LoadClassExtraMethod implements ServiceExtraMethodHandler<Object> {
        @Override
        public Object invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
            // MyBean is Datatype defined in OpenL
            Class<?> myBeanClass = Thread.currentThread().getContextClassLoader()
                        .loadClass("org.openl.generated.beans.MyBean");
            Object myBean = myBeanClass.newInstance();
            // … Do some mapping below and then return result
            return myBean;
        }
    }
```

**Note:** Java byte code does not have argument names in interfaces, so they are named as 'arg0', 'arg1', and so on. To request more meaningful names for parameters, use the @ org.openl.rules.ruleservice.core.annotations.Name annotation together with @ServiceExtraMethod.

Use the org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware and org.openl.rules.ruleservice.core.interceptors.IOpenClassAware interfaces if a reference to the compiled IOpenClass or IOpenMember object is required in an interceptor implementation class.

#### Method Return Type Customization through Annotations

By default, OpenL Tablets applies the org.openl.rules.ruleservice.core.interceptors.converters.SPRToPlainConverterAdvice interceptor to all spreadsheet table methods that return SpreadsheetResult. These annotations transform the spreadsheet table result to the generated Java bean and return it instead of SpreadsheetResult.

**Note:**  If any interceptor is used on the method, the SPRToPlainConverterAdvice interceptor must be added manually to keep default behavior**.**

To change default behavior, define `@org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor `with an empty value on the method to return SpreadsheetResult.

#### REST Endpoint Customization through Annotations

By default, URLs and HTTP method type for methods are determined automatically by the system. The path for the methods equals the corresponding service method name, and HTTP method type depends on used arguments: if the service method has at least one argument, a HTTP method type is set to POST, otherwise, to GET.

The following JAX-RS annotations can be used to override the default behavior of service method publishing:

| Annotation | Import details             |
|------------|----------------------------|
| `@POST`    | `import javax.ws.rs.POST;` |
| `@GET`     | `import javax.ws.rs.GET;`  |
| `@Path`    | `import javax.ws.rs.Path;` |

-   `@POST` annotation overrides a default method type.

Service methods annotated `@POST` accepts only POST requests. Usage example is as follows:

```java
@POST
MyResponse someMethod();
```

-   `@GET` annotation overrides a default method type.

Service method annotated `@GET` accepts only GET requests. Usage example is as follows:

```java
@GET
MyResponse someMethod(MyType myType);
```

-   `@Path` annotation overrides a default URL method path.

Usage example is as follows:

```java
@Path(“/customPrefix/someMethod”)
MyResponse someMethod(MyType myType);
```

Required Maven dependency is as follows:

```xml
<dependency>
    <groupId>jakarta.ws.rs</groupId>
    <artifactId>jakarta.ws.rs-api</artifactId>
    <version>2.1.5</version>
    <scope>provided</scope>
</dependency>
```

**Note:** It is not necessary to declare pairs of `@POST` + `@Path` or `@GET` + `@Path` because OpenL Tablets provides the capability to define a single annotation and generate the other one automatically.

All other JAX-RS annotations, such as `@PUT`, `@DELETE`, `@QueryParam`, and `@PathParam,` are also supported by OpenL Tablets. For more information on JAX-RS annotation, see <https://docs.oracle.com/javaee/7/api/javax/ws/rs/package-summary.html>.

#### Customization through Annotations for Dynamic Generated Interfaces

Annotation customization can be used for dynamically generated interfaces. This feature is only supported for projects that contain the `rules-deploy.xml `deployment configuration file. To enable customization through annotation, proceed as follows:

1.  Add the `annotationTemplateClassName` tag to the `rules-deploy.xml` file*.*
        
    An example is as follows:
        
    ```xml
    <rules-deploy>
        <isProvideRuntimeContext>true</isProvideRuntimeContext>
        <serviceName>dynamic-interface-test3</serviceName>
        <annotationTemplateClassName>org.openl.ruleservice.dynamicinterface.test.MyTemplateClass</annotationTemplateClassName>
        <url></url>
    </rules-deploy> 
    ```
        
1.  Define a template interface with the annotated methods with the same signature as in a generated dynamic interface.

This approach supports replacing argument types in the method signature with types assignable from generated types in the generated interface.

**Example:** SubType is a subclass of class MyType. Consider the following methods are generated in the generated interface:

```java
void someMethod(IRulesRuntimeContext context, MyType myType);
void someMethod(IRulesRuntimeContext context, SubType otherType);
```

Add an annotation to the first method using the same method signature in the template interface as follows:

```java
@ServiceCallAfterInterceptor(value = { MyAfterAdvice.class })
void someMethod(IRulesRuntimeContext context, MyType myType);
```

If the `MyType` class is also generated at runtime, use a super type of the `MyType` class. An example is as follows:

```java
@ServiceCallAfterInterceptor(value = { MyAfterAdvice.class })
void someMethod(IRulesRuntimeContext context, @RulesType("MyType") Object myType);
```

This example uses the `@org.openl.rules.ruleservice.core.interceptors.RulesType` annotation. If this annotation is missed, this template method is applied to both methods because `Object` is assignable from both types `MyType `and `SubType`.

The `@RulesType` annotation value accepts the following:

-   canonical class name
-   datatype name
-   custom SpreadsheetResult name

Use this annotation if more details are required to define a template method.

**Note:** A user can also use class level annotations for a dynamically generated class. It can be useful for JAX-WS or JAX-RS interface customization.

### Customization of Log Requests to OpenL Rule Services and Their Responds in a Storage

This section describes advanced customization for logging requests to OpenL Rule Services and their responds in a storage if different parts of the input and output data must be stored separately. It also describes how to customize a structure of tables and indexes in a storage.

The following topics are included:

-   [Storage Service for Log Requests and Their Responds](#storage-service-for-log-requests-and-their-responds)
-   [Customization for Apache Cassandra](#customization-for-apache-cassandra)
-   [Customization for the Relational Database](#customization-for-the-relational-database)
-   [Customization for Hive](#customization-for-hive)

#### Storage Service for Log Requests and Their Responds

This section describes storage service used for log requests and responds and includes the following topics:

-   [Log Request and Response Storage Service Overview](#log-request-and-response-storage-service-overview)
-   [Collecting Data from Requests and Their Responds and Populating Custom Values](#collecting-data-from-requests-and-their-responds-and-populating-custom-values)
-   [Log Requests and Their Responds Customization Using Annotations](#log-requests-and-their-responds-customization-using-annotations)

##### Log Request and Response Storage Service Overview

OpenL Rule Services supports Apache Cassandra and relational database storages to log request and their responds out of the box. This part of the system is designed customizable and extendable via the org.openl.rules.ruleservice.storelogdata.StoreLogDataService interface to support the third-party storages.

The StoreLogDataService interface has the following methods:

| Method                               | Description                                            |
|--------------------------------------|--------------------------------------------------------|
| boolean isEnabled()                  | Identifies whether the log storing service is enabled. |
| void save(StoreLogData storeLogData) | Saves storeLogData data to a storage.                  |

The implementation class of this interface must be registered in the application Spring context. The system discovers all implementation of the interface automatically and uses all found services at the same time.

`org.openl.rules.ruleservice.storelogdata.StoreLogData` is a class that contains all available data from the request and respond. This class has the `getCustomValues()` method that returns a map for interested values that can be stored separately from request payload.

Custom implementation of the StoreLogDataService interface supports all features described in this document.

Annotation on the called method `@org.openl.rules.ruleservice.storelogdata.annotation.SkipFaultStoreLogData` instructs the system to skip storing fault requests and their responds in a storage.

##### Collecting Data from Requests and Their Responds and Populating Custom Values

Populating custom values in the StoreLogData object and collecting data for service methods is defined using the @org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData annotation.

| Attribute                 | Description                                                                                                                                                                                                                                                                                                                                   |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| value                   | Mandatory reference to the StoreLogDataAdvice interface implementation. <br/>The implementation class defines which data is collected.                                                                                                                                                                                                             |
| bindToServiceMethodAdvice | Optional reference to an implementation of the ServiceMethodAdvice interface. <br/>It defines that the implementation of the theStoreLogDataAdvice interface must be invoked <br/>before or after the corresponding ServiceMethodAdvice implementation. <br/>It is used when required data for collecting is not more available after result transformation. |
| before                  | Optional attribute specifying the order of the called data collecting advice. <br/>If the bindToServiceMethodAdvice attribute is present, before determines the advice execution relative to the defined interceptor, <br/>otherwise relative to the base method. <br/>The default value is false, that is, execution happens after method or interceptor.   |

Implement a single method in the StoreLogDataAdvice interface for collecting data to be used along with the `@Value` annotation in entities or directly from StoreLogData.getCustomValues().

Using more than one @PrepareStoreLogData to logically decouple the code of collecting a data is allowed for the same method.

All these annotations can be used on fields or on getter or setter methods in entity classes.

The org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice interface has only one method to implement. An example is as follows:

```java
public class CollectDataStoreLogDataAdvice implements StoreLogDataAdvice {
   @Override
   public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
       values.put(“state", ((CalculationResult)result).getState());
   }
}
```

To programmatically control whether a call to the service must be stored or skipped, use the org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder.get().ignore() line of code in implementation of StoreLogDataAdvice.

If compound object serialization to string is required in StoreLogDataAdvice, use the org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware interface. It injects the org.openl.rules.ruleservice.storelogdata.ObjectSerializer instance automatically via the void setObjectSerializer(ObjectSerializer objectSerializer) method. ObjectSerializer provides functionality to serialize an object to a string with the same mechanism used in the invoked publisher. For example, it produces a JSON string for REST or Kafka services.

##### Log Requests and Their Responds Customization Using Annotations

OpenL Rule Services has annotations for mapping requests and their responds data to entity classes. The org.openl.rules: org.openl.rules.ruleservice.ws.storelogdata Maven dependency is required for the log requests and their respond annotations. Use the provided scope for dependency as it already exists in OpenL Rule Services and it must not be included in the deployment distributive to avoid class duplication in ClassLoader.

The org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper class maps OpenL Tablets annotations to the entity class.

The following annotations located in the org.openl.rules.ruleservice.storelogdata.annotation package are supported:

| **Annotation**       | **Field Type**  | **Description**                                                                                                                                                    |
|----------------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `IncomingTime`       | `ZonedDateTime` | Incoming request time.                                                                                                                                             |
| `OutcomingTime`      | `ZonedDateTime` | Outgoing response time.                                                                                                                                            |
| `MethodName`         | `String`        | Method of a service that is called.                                                                                                                                |
| `ServiceName`        | `String`        | Deployment service name that is called.                                                                                                                            |
| `Publisher`          | `String`        | Request source, such as web service or REST service or Kafka.                                                                                                      |
| `Request`            | `String`        | Request body, such as JSON for the REST service, and message body for Kafka.                                                                                       |
| `Response`           | `String`        | Response body, such as JSON for REST service, and message body for Kafka.                                                                                          |
| `Url`                | `String`        | URL of the request if available.                                                                                                                                   |
| `Value`              | `Object`        | Value from the map that is returned by StoreLogData .getCustomValues()                                                                                             |
| `KafkaMessageHeader` | `byte[]`        | Kafka message header data. The value attribute with a defined header name is required. <br/>The type attribute is used to define a producer or consumer message to use. |

All annotations described in this section have an optional converter attribute for converting a collected type into the required field type. Use implementation of the org.openl.rules.ruleservice.storelogdata.Converter interface for the convertor attribute. A usage example of this interface is as follows:

```java
public final class ZonedDataTimeToDateConvertor implements Converter<ZonedDateTime, Date> {
    @Override
    public Date apply(ZonedDateTime value) {
        return value != null ? Date.from(value.toInstant()) : null;
    }
}
```

#### Customization for Apache Cassandra

This section describes customization for Apache Cassandra and automatically creating a table schema for entity classes. The following topics are described:

-   [Log Requests and Responds Customization for Apache Cassandra](#log-requests-and-responds-customization-for-apache-cassandra)
-   [Automatically Creating a Cassandra Table Schema Creation for Entity Classes](#automatically-creating-a-cassandra-table-schema-for-entity-classes)

##### Log Requests and Responds Customization for Apache Cassandra

Service storing log requests and their responds for Apache Cassandra requires a Cassandra driver version 4.x. The Cassandra driver uses a new mapping model between object in the code and a table in a database. For more information on mapping, see <https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/>. The nutshell working with this model assumes that there are three objects: Entity, Dao, and Mapper interface.

For a method, to enable logging requests and their responds to Apache Cassandra, annotate calling method with the `@org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra` annotation. The annotation has an optional attribute that obtains entity classes. If `@StoreLogDataToCassandra` is used with an empty value, the default table described in [Storing Log Records in Apache Cassandra](#storing-log-records-in-apache-cassandra) is used. If more than one entity class is used in the value attribute for the `@StoreLogDataToCassandra` annotation, the system splits data and stores it in multiple Cassandra tables.

An entity is a simple data container that represents a row in the product table. For more information on entities, see <https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/entities/>.

Cassandra entity example is as follows:

```java
@Entity
@EntitySupport(PersonOperations.class)
@CqlName("person")
public class Person {
   @PartitionKey()
   @Value("id")
   private String id;
   @PartitionKey(1)
   @Value(value = "birthday")
   private ZonedDateTime birthday;
   @Request
   private String request;
   @Response
   private String response;        
…
}
```

A **data access object** (DAO) defines a set of query methods to insert entities into a storage. For more information on DAO, see <https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/daos/>.

DAO interface example to insert a Person entity is as follows:

```java
@Dao
public interface PersonDao {
   @Insert
   CompletionStage<Void> insert(Person entity);
}
```

Mapper interface is a top-level entry point for mapper features used to obtain DAO instances. For more information on Mapper interface, see <https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/mapper/>.

Mapper example that obtains PersonDao is as follows:

```java
@Mapper
public interface PersonMapper {
   @DaoFactory
   PersonDao getDao();
}
```

Generate an implementation for these interfaces to use it at runtime. To generate the code annotation processor, add it to the Maven build script. For more information on how to configure the annotation processor, see <https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/config/>.

An example of using Maven plugin to generate implementations is as follows:

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>com.datastax.oss</groupId>
                <artifactId>java-driver-mapper-processor</artifactId>
                <version>${cassandra.driver.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>      
```

The @org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntitySupport annotation is used to define a class that instantiates a mapper instance with generated mapper builder and implements insert operation. This annotation must be used on the entity class as follows:

```java
@Entity
@EntitySupport(PersonOperations.class)
@CqlName("person")
public class Person {
  …
}
public class PersonOperations implements EntityOperations<PersonDao, Person> {
   @Override
   public PersonDao buildDao(CqlSession cqlSession) throws DaoCreationException {
       PersonMapper entityMapper = new PersonMapperBuilder(cqlSession).build();
       return entityMapper.getDao();
   }
   @Override
   public CompletionStage<Void> insert(PersonDao, Person person) {
       return personDao.insert(person);
   }
}
```

##### Automatically Creating a Cassandra Table Schema for Entity Classes

The system uses the ClassLoader CQL scripts that are located in the same package and have the same names as entity classes and the `.cql` file extension to create Cassandra schema tables automatically on application launch.

Cassandra identifiers, such as keyspace, table, and column names, are case-insensitive by default. There are several naming strategies to map names and fields. By default , it is [`SNAKE_CASE_INSENSITIVE`](https://docs.datastax.com/en/drivers/java/4.3/com/datastax/oss/driver/api/mapper/entity/naming/NamingConvention.html#SNAKE_CASE_INSENSITIVE) that divides the Java name into words, splits on upper-case characters, lower-cases everything concatenates the words with underscore separators, and makes the result a case-insensitive CQL name. For example, Product =\> product, productId =\> product_id.

The default strategy can be modified. For more information on naming strategies, see [https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/entities/\#naming-strategy](https://docs.datastax.com/en/developer/java-driver/4.3/manual/mapper/entities/#naming-strategy).

An example is as follows:

```sql
CREATE TABLE IF NOT EXISTS person(
  id text,
  birthday timestamp,
  request text,
  response text,
  …
}
```

#### Customization for the Relational Database

OpenL Rule Services uses Hibernate implementation to store requests and their responds in the relational database.

To enable logging requests and their responses to the relational database, mark the method with the org.openl.rules.ruleservice.storelogdata.db.annotation.StoreLogDataToDB annotation. It resembles @StoreLogDataToCassandra described in [Log Requests and Responds Customization for Apache Cassandra](#log-requests-and-responds-customization-for-apache-cassandra), and it has entity classes as optional attributes.

If entity classes are not defined in @StoreLogDataToDB, all records are stored in `openl_log_data`.

A custom relational database entity example is as follows:

```java
@Entity(name = "person")
public class Person {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_generator")
    @SequenceGenerator(name = "person_generator", sequenceName = "openl_log_data_generator", allocationSize = 50)
    private Long id;
    
    @IncomingTime
    private Date incomingTime;

    @OutcomingTime(converter = ZonedDataTimeToDateConvertor.class)
    private ZonedDateTime outcomingTime;

    @Request
    private String requestBody;

    @Response
    private String responseBody;

}
```

#### Customization for Hive

OpenL Rule Services stores its requests and responds in Hive.

To enable logging requests and their responses to Hive, mark the method with the `org.openl.rules.ruleservice.storelogdata.hive.annotation.StoreLogDataToHive` annotation. It resembles `@StoreLogDataToCassandra` described in [Log Requests and Responds Customization for Apache Cassandra](#log-requests-and-responds-customization-for-apache-cassandra), and it has entity classes as optional attributes.

If entity classes are not defined in `@StoreLogDataToHive`, all records are stored in the table described in [Storing Log Records in Hive](#storing-log-records-in-hive).

If only one entity class is defined, for example, `@StoreLogDataToHive(CustomHiveEntity.class)`, the system uses a table defined in the custom entity.

If multiple entity classes are defined, for example, `@StoreLogDataToHive(HiveEntity1.class, HiveEntity2.class, ..., HiveEntityN.class)`, the system splits data into multiple Hive tables.

Custom Hive entity example is as follows:

```java
@Entity("person_data")
public class Person {
    
    @Value(converter = RandomUUID.class)
    private String id;

    @IncomingTime
    private ZonedDateTime incomingTime;

    @OutcomingTime
    private ZonedDateTime  outcomingTime;

    @Request
    private String request;

    @Response
    private String response;

    @ServiceName
    private String serviceName;

    @MethodName
    private String methodName;

    @Publisher
    private String publisherType;

    @Url
    private String url;
}
```

Entity annotation identifies a domain object to be persisted in Hive.

The system uses the ClassLoader SQL scripts which are located in the same package and have the same names as entity classes and the .sql file extension to create Hive table automatically on application launch. For more information on how to enable this feature, see [Storing Log Records in Hive](#storing-log-records-in-hive).


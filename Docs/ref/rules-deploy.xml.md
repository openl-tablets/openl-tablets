# OpenL Tablets — Rules Deployment Descriptor (`rules-deploy.xml`)

```xml
<rules-deploy>
    <!-- Indicates whether to provide a runtime context object to the service implementation. DEPRECATED -->
    <isProvideRuntimeContext>false</isProvideRuntimeContext>
    <!-- Indicates whether to provide variations of rules for different contexts. DEPRECATED -->
    <isProvideVariations>false</isProvideVariations>
    <!-- Unique logical name of the service to be exposed by OpenL Rule Services. -->
    <serviceName>my-pricing-service</serviceName>
    <!-- Base URL path segment for the service endpoint -->
    <url>pricing/common</url>
    <!-- Service version string to append to the base URL -->
    <version>2.0</version>
    <!-- Comma-separated list of deployment groups for selective deployment -->
    <groups>group1,group2</groups>
    <!-- RMI publisher configuration. DEPRECATED -->
    <rmiServiceClass>com.example.IPricingServiceRMI</rmiServiceClass>
    <!-- RMI binding name. DEPRECATED -->
    <rmiName>PricingServiceRMI</rmiName>
    <!-- Fully-qualified Java interface that the generated service will implement. -->
    <serviceClass>com.example.IPricingService</serviceClass>
    <!-- Fully-qualified class name of an AOP interceptor to apply to the service. DEPRECATED, Use <annotationTemplateClassName> -->
    <interceptingTemplateClassName>com.example.MyAnnotationTemplate</interceptingTemplateClassName>
    <!-- Fully-qualified class name of an annotation template to apply to the generated service class. -->
    <annotationTemplateClassName>com.example.MyAnnotationTemplate</annotationTemplateClassName>
    <!-- Publishers to expose the service through. Multiple publishers can be active simultaneously. -->
    <!-- If publishers is omitted, then defaults is `RESTFUL` according to the OpenL Rule Services config. -->
    <publishers>
        <!-- RMI publisher. DEPRECATED -->
        <publisher>RMI</publisher>
        <!-- SOAP web service publisher. DEPRECATED -->
        <publisher>WEBSERVICE</publisher>
        <publisher>KAFKA</publisher>
        <publisher>RESTFUL</publisher>
    </publishers>
    <!-- Arbitrary key-value pairs for custom configuration for the given deployment -->
    <configuration>
        <entry>
            <string>rootClassNamesBinding</string>
            <string>com.example.custom.mixin.JacksonMixIn, com.example.beans.Animals</string>
        </entry>
        <entry>
            <string>jackson.propertyNamingStrategy</string>
            <string>org.openl.rules.serialization.spr.LowerCamelCaseStrategy</string>
        </entry>
        <entry>
            <string>jackson.defaultDateFormat</string>
            <string>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</string>
        </entry>
        <entry>
            <string>jackson.serializationInclusion</string>
            <string>NON_EMPTY</string>
        </entry>
        <entry>
            <string>jackson.typingPropertyName</string>
            <string>_type</string>
        </entry>
        <entry>
            <string>jackson.simpleClassNameAsTypingPropertyValue</string>
            <string>true</string>
        </entry>
        <entry>
            <string>jackson.defaultTypingMode</string>
            <string>DISABLED</string>
        </entry>
        <entry>
            <string>jackson.failOnEmptyBeans</string>
            <string>false</string>
        </entry>
        <entry>
            <string>jackson.caseInsensitiveProperties</string>
            <string>false</string>
        </entry>
    </configuration>
</rules-deploy>
```

## Appendix A: Using OpenL Tablets REST Services from Java Code

This section describes how to write a client code that invokes OpenL Tablets REST services projects. Another way can be used to invoke services, but it is recommended to use Apache CXF framework to prevent additional effort for data binding.

Build the client's JSON mapper with `JacksonObjectMapperFactoryBean` from the `org.openl.rules.jackson` module, configured as the service is, so that the client reads and writes the type of a value the way the service does. The following example calls a service method with the JSON content type:

```java
JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
// The classes the service binds with databinding.rootClassNames
jacksonObjectMapperFactoryBean.setOverrideTypes(Set.of(SomeClass.class.getName()));
// The default value of ruleservice.jackson.jsonTypeInfoId
jacksonObjectMapperFactoryBean.setJsonTypeInfoId(JsonTypeInfo.Id.NAME);
ObjectMapper mapper = jacksonObjectMapperFactoryBean.createJacksonObjectMapper();

WebClient webClient = WebClient.create("http://localhost:8080/my-service",
        List.of(new JacksonJsonProvider(mapper)));

webClient.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

SomeClass result = webClient.path("myMethod").get(SomeClass.class);
```

`WebClient` is `org.apache.cxf.jaxrs.client.WebClient`, `JacksonJsonProvider` is
`com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider`, and `MediaType` is `jakarta.ws.rs.core.MediaType`.

**Note**: If you use POST request for more than one argument, create a DTO that contains field with method argument names and send this DTO object via `webClient.post()` method.

## Appendix A: Using OpenL Tablets REST Services from Java Code

This section describes how to write a client code that invokes OpenL Tablets REST services projects. Another way can be used to invoke services, but it is recommended to use Apache CXF framework to prevent additional effort for data binding.

The following example illustrates client code generation for the JSON content type:

```java
JacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
jacksonObjectMapperFactoryBean.setEnableDefaultTyping(true);
Set<String> overrideTypes = new HashSet<String>();
overrideTypes.add(SomeClass.class.getName());
        
jacksonObjectMapperFactoryBean.setOverrideTypes(overrideTypes);
ObjectMapper mapper = jacksonObjectMapperFactoryBean.createJacksonDatabinding();
        
final JacksonJsonProvider jsonProvider = new JacksonJsonProvider();

WebClient webClient = WebClient.create(#REST service url#,
            new ArrayList<Object>() {
                private static final long serialVersionUID = 5636807402394548461L;
                {
                    add(jsonProvider);
                }
            });
        
webClient.type(MediaType.APPLICATION_JSON);
        
Response response = webClient.get();
```

**Note**: If you use POST request for more than one argument, create a DTO that contains field with method argument names and send this DTO object via `webClient.post()` method.


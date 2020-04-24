package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.List;

@SuppressWarnings("rawtypes")
public abstract class AbstractSwaggerAndOpenApiObjectMapperHack implements SwaggerAndOpenApiObjectMapperHack {
    protected List converters;
    protected List<Object> oldConverters;

    @SuppressWarnings("unchecked")
    public void revert() {
        converters.clear();
        converters.addAll(oldConverters);
    }
}

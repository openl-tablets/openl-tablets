package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;

@SuppressWarnings("rawtypes")
public class OpenApiObjectMapperHack extends AbstractSwaggerAndOpenApiObjectMapperHack {

    public OpenApiObjectMapperHack() {
        try {
            ModelConverters modelConverters = ModelConverters.getInstance();
            Field convertersField = ModelConverters.class.getDeclaredField("converters");
            convertersField.setAccessible(true);
            this.converters = (List) convertersField.get(modelConverters);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void apply(ObjectMapper objectMapper) {
        List<Object> hackedConverters = new ArrayList<>();
        oldConverters = new ArrayList<>();
        for (Object converter : converters) {
            oldConverters.add(converter);
            hackedConverters.add(converter instanceof ModelResolver ? new ModelResolver(objectMapper) : converter);
        }
        converters.clear();
        converters.addAll(hackedConverters);
    }

}

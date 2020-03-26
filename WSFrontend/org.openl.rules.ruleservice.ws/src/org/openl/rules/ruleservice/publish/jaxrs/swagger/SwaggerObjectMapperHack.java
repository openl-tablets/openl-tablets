package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.converter.ModelConverters;
import io.swagger.jackson.AbstractModelConverter;

@SuppressWarnings("rawtypes")
public class SwaggerObjectMapperHack {

    private final List converters;
    private final Field swaggerObjectMapperField;

    private final Map<Integer, Object> oldMappers;

    public SwaggerObjectMapperHack() {
        try {
            ModelConverters modelConverters = ModelConverters.getInstance();
            Field convertersField = ModelConverters.class.getDeclaredField("converters");
            convertersField.setAccessible(true);
            this.converters = (List) convertersField.get(modelConverters);

            Field swaggerMapperField = AbstractModelConverter.class.getDeclaredField("_mapper");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(swaggerMapperField, swaggerMapperField.getModifiers() & ~Modifier.FINAL);
            swaggerMapperField.setAccessible(true);

            this.swaggerObjectMapperField = swaggerMapperField;
            this.oldMappers = new HashMap<>();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public void apply(ObjectMapper objectMapper) throws IllegalAccessException {
        Iterator it = converters.iterator();
        int i = 0;
        while (it.hasNext()) {
            final Object modelResolver = it.next();
            if (modelResolver instanceof AbstractModelConverter) {
                oldMappers.put(i, swaggerObjectMapperField.get(modelResolver));
                try {
                    swaggerObjectMapperField.set(modelResolver, objectMapper);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            i++;
        }
    }

    public void revert() {
        Iterator it = converters.iterator();
        int i = 0;
        while (it.hasNext()) {
            final Object modelResolver = it.next();
            if (modelResolver instanceof AbstractModelConverter) {
                Object oldObjectMapper = oldMappers.get(i);
                try {
                    swaggerObjectMapperField.set(modelResolver, oldObjectMapper);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            i++;
        }
    }

}

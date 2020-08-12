package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.media.Schema;

public class OpenApiInheritanceFixConverter implements ModelConverter {
    private final List<ModelConverter> converters;
    private final ObjectMapper objectMapper;

    public OpenApiInheritanceFixConverter(ObjectMapper objectMapper, List<ModelConverter> converters) {
        this.converters = Objects.requireNonNull(converters, "converters cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        ModelConverterContextImpl modelConverterContext = new ModelConverterContextImpl(converters);
        try {
            modelConverterContext.resolve(annotatedType);
            fixParentClasses(modelConverterContext);
            return modelConverterContext.resolve(annotatedType);
        } finally {
            for (Map.Entry<String, Schema> definedModel : modelConverterContext.getDefinedModels().entrySet()) {
                context.defineModel(definedModel.getKey(), definedModel.getValue());
            }
        }
    }

    private void fixParentClasses(ModelConverterContextImpl modelConverterContext) {
        HashMap<AnnotatedType, Schema> modelByType = getModelByType(modelConverterContext);
        Set<Class<?>> baseClasses = new HashSet<>();
        for (Map.Entry<AnnotatedType, Schema> entry : modelByType.entrySet()) {
            Class<?> clazz = null;
            if (entry.getKey().getType() instanceof JavaType) {
                JavaType javaType = (JavaType) entry.getKey().getType();
                clazz = javaType.getRawClass();
            } else if (entry.getKey().getType() instanceof Class) {
                clazz = (Class<?>) entry.getKey().getType();
            }
            if (clazz != null) {
                Class<?> baseClass = InheritanceFixConverterHelper.extractBaseClass(clazz, objectMapper);
                if (baseClass != clazz) {
                    baseClasses.add(baseClass);
                }
            }
        }
        for (Class<?> type : baseClasses) {
            modelConverterContext.resolve(new AnnotatedType().type(type));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static HashMap<AnnotatedType, Schema> getModelByType(ModelConverterContextImpl context) {
        try {
            Field modelByTypeField = ModelConverterContextImpl.class.getDeclaredField("modelByType");
            modelByTypeField.setAccessible(true);
            return (HashMap<AnnotatedType, Schema>) modelByTypeField.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}

package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.converter.ModelConverterContextImpl;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;

public class SwaggerInheritanceFixConverter implements ModelConverter {
    private final ObjectMapper objectMapper;
    private final ModelConverterContextImpl modelConverterContext;

    public SwaggerInheritanceFixConverter(ObjectMapper objectMapper, List<ModelConverter> converters) {
        Objects.requireNonNull(converters, "converters cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.modelConverterContext = new ModelConverterContextImpl(converters);
    }

    @Override
    public Property resolveProperty(Type type,
            ModelConverterContext context,
            Annotation[] annotations,
            Iterator<ModelConverter> chain) {
        try {
            modelConverterContext.resolveProperty(type, annotations);
            fixParentClasses(modelConverterContext);
            return modelConverterContext.resolveProperty(type, annotations);
        } finally {
            for (Map.Entry<String, Model> definedModel : modelConverterContext.getDefinedModels().entrySet()) {
                context.defineModel(definedModel.getKey(), definedModel.getValue());
            }
        }
    }

    private void fixParentClasses(ModelConverterContextImpl modelConverterContext) {
        Map<Type, Model> modelByType = getModelByType(modelConverterContext);
        Set<Class<?>> parentClasses = new LinkedHashSet<>();
        for (Map.Entry<Type, Model> entry : modelByType.entrySet()) {
            Class<?> clazz = null;
            if (entry.getKey() instanceof JavaType) {
                JavaType javaType = (JavaType) entry.getKey();
                clazz = javaType.getRawClass();
            } else if (entry.getKey() instanceof Class) {
                clazz = (Class<?>) entry.getKey();
            }
            Class<?> parentClass = InheritanceFixConverterHelper.extractParentClass(clazz, objectMapper);
            if (parentClass != null) {
                parentClasses.add(parentClass);
            }
        }
        for (Class<?> parentClass : parentClasses) {
            modelConverterContext.resolve(parentClass);
        }
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        try {
            modelConverterContext.resolve(type);
            fixParentClasses(modelConverterContext);
            return modelConverterContext.resolve(type);
        } finally {
            for (Map.Entry<String, Model> definedModel : modelConverterContext.getDefinedModels().entrySet()) {
                context.defineModel(definedModel.getKey(), definedModel.getValue());
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static Map<Type, Model> getModelByType(ModelConverterContextImpl context) {
        try {
            Field modelByTypeField = ModelConverterContextImpl.class.getDeclaredField("modelByType");
            modelByTypeField.setAccessible(true);
            return (Map<Type, Model>) modelByTypeField.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}

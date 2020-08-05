package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private final List<ModelConverter> converters;
    private final ObjectMapper objectMapper;

    public SwaggerInheritanceFixConverter(ObjectMapper objectMapper, List<ModelConverter> converters) {
        this.converters = Objects.requireNonNull(converters, "converters cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public Property resolveProperty(Type type,
            ModelConverterContext context,
            Annotation[] annotations,
            Iterator<ModelConverter> chain) {
        ModelConverterContextImpl modelConverterContext = new ModelConverterContextImpl(converters);
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
        HashMap<Type, Model> modelByType = getModelByType(modelConverterContext);
        Set<Class<?>> baseClasses = new HashSet<>();
        for (Map.Entry<Type, Model> entry : modelByType.entrySet()) {
            Class<?> clazz = null;
            if (entry.getKey() instanceof JavaType) {
                JavaType javaType = (JavaType) entry.getKey();
                clazz = javaType.getRawClass();
            } else if (entry.getKey() instanceof Class) {
                clazz = (Class<?>) entry.getKey();
            }
            if (clazz != null) {
                Class<?> baseClass = InheritanceFixConverterHelper.extractBaseClass(clazz, objectMapper);
                if (baseClass != null) {
                    baseClasses.add(baseClass);
                }
            }
        }
        for (Class<?> baseClass : baseClasses) {
            modelConverterContext.resolve(baseClass);
        }
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        ModelConverterContextImpl modelConverterContext = new ModelConverterContextImpl(converters);
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
    private static HashMap<Type, Model> getModelByType(ModelConverterContextImpl context) {
        try {
            Field modelByTypeField = ModelConverterContextImpl.class.getDeclaredField("modelByType");
            modelByTypeField.setAccessible(true);
            return (HashMap<Type, Model>) modelByTypeField.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}

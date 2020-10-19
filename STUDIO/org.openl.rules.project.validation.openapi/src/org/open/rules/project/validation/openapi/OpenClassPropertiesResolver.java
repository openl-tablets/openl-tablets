package org.open.rules.project.validation.openapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;

class OpenClassPropertiesResolver {
    private final Context context;
    private final Map<IOpenClass, Map<String, IOpenField>> cache = new HashMap<>();

    public OpenClassPropertiesResolver(Context context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    private IOpenField resolveOpenFieldByPropertyName(IOpenClass openClass, String propertyName) {
        BeanPropertyDefinition beanPropertyDefinition = resolveOpenBeanPropertyDefinitionByPropertyName(openClass,
            propertyName);
        if (beanPropertyDefinition != null) {
            String fieldName;
            if (beanPropertyDefinition.getAccessor() instanceof AnnotatedField) {
                fieldName = beanPropertyDefinition.getAccessor().getName();
            } else {
                String getterName = beanPropertyDefinition.getAccessor().getName();
                fieldName = ClassUtils.toFieldName(getterName);
            }
            //Fail safe lines if class is not handled by openl (imported)
            IOpenField openField = openClass.getField(fieldName);
            if (openField != null) {
                return openField;
            }
            openField = openClass.getField(ClassUtils.capitalize(fieldName));
            if (openField != null) {
                return openField;
            }
            return openClass.getField(StringUtils.uncapitalize(fieldName));
        }
        return null;
    }

    private BeanPropertyDefinition resolveOpenBeanPropertyDefinitionByPropertyName(IOpenClass openClass,
            String propertyName) {
        final BeanDescription beanDesc = context.getObjectMapper()
            .getSerializationConfig()
            .introspect(TypeFactory.defaultInstance().constructType(openClass.getInstanceClass()));
        for (BeanPropertyDefinition beanPropertyDefinition : beanDesc.findProperties()) {
            if (Objects.equals(propertyName, beanPropertyDefinition.getName())) {
                return beanPropertyDefinition;
            }
        }
        return null;
    }

    public IOpenField findFieldByPropertyName(IOpenClass openClass, String propertyName) {
        Map<String, IOpenField> propertiesCache = cache.computeIfAbsent(openClass, e -> new HashMap<>());
        return propertiesCache.computeIfAbsent(propertyName, e -> resolveOpenFieldByPropertyName(openClass, e));
    }
}

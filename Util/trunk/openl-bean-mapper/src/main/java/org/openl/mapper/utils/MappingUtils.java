package org.openl.mapper.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dozer.CustomConverter;
import org.dozer.factory.XMLBeanFactory;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeDefinition;
import org.dozer.loader.api.TypeMappingBuilder;
import org.dozer.loader.api.TypeMappingOption;
import org.openl.mapper.mapping.Bean2BeanMappingDescriptor;
import org.openl.mapper.mapping.Field2FieldMappingDescriptor;

public class MappingUtils {

    public static BeanMappingBuilder createDozerMapping(final Bean2BeanMappingDescriptor mapping) {

        BeanMappingBuilder beanMappingBuilder = new BeanMappingBuilder() {
            protected void configure() {

                TypeDefinition typeADef = type(mapping.getClassA());
                if (mapping.isClassAXmlBean()) {
                    typeADef.beanFactory(XMLBeanFactory.class);
                }
                TypeDefinition typeBDef = type(mapping.getClassB());
                if (mapping.isClassBXmlBean()) {
                    typeBDef.beanFactory(XMLBeanFactory.class);
                }

                List<TypeMappingOption> options = new ArrayList<TypeMappingOption>();
                options.add(oneWay());
                options.add(wildcard(false));

                TypeMappingBuilder typeMappingBuilder = mapping(typeADef, typeBDef, options
                    .toArray(new TypeMappingOption[options.size()]));

                for (final Field2FieldMappingDescriptor fieldMapping : mapping.getFieldMappings()) {
                    if (fieldMapping.getConverter() != null) {
                        String converterId = fieldMapping.getConverter().getConverterId();
                        typeMappingBuilder.fields(fieldMapping.getFieldA(), fieldMapping.getFieldB(),
                            customConverterId(converterId));
                    } else {
                        typeMappingBuilder.fields(fieldMapping.getFieldA(), fieldMapping.getFieldB());
                    }
                }
            }
        };

        return beanMappingBuilder;
    }

    public static Map<String, CustomConverter> getCustomConvertersMap(Bean2BeanMappingDescriptor mapping) {

        Map<String, CustomConverter> map = new HashMap<String, CustomConverter>();

        for (final Field2FieldMappingDescriptor fieldMapping : mapping.getFieldMappings()) {
            if (fieldMapping.getConverter() != null) {
                map.put(fieldMapping.getConverter().getConverterId(), fieldMapping.getConverter().getInstance());
            }
        }

        return map;
    }

}

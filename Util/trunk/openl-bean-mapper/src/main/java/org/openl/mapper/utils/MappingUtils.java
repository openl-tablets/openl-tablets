package org.openl.mapper.utils;

import java.util.HashMap;
import java.util.Map;

import org.dozer.CustomConverter;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeMappingBuilder;
import org.openl.mapper.mapping.Bean2BeanMappingDescriptor;
import org.openl.mapper.mapping.Field2FieldMappingDescriptor;

public class MappingUtils {

    public static BeanMappingBuilder createDozerMapping(final Bean2BeanMappingDescriptor mapping) {

        BeanMappingBuilder beanMappingBuilder = new BeanMappingBuilder() {
            protected void configure() {

                TypeMappingBuilder typeMappingBuilder = mapping(type(mapping.getClassA()), type(mapping.getClassB()),
                    oneWay(), wildcard(false));

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
            if (fieldMapping.getConverter()!= null) {
                map.put(fieldMapping.getConverter().getConverterId(), fieldMapping.getConverter().getInstance());
            }
        }
        
        return map;
    }
  
}

package org.openl.mapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dozer.CustomConverter;
import org.dozer.DozerBeanMapper;
import org.dozer.MappingException;
import org.openl.mapper.mapping.Bean2BeanMappingDescriptor;
import org.openl.mapper.mapping.MappingProcessor;
import org.openl.mapper.utils.MappingUtils;

public class RulesBeanMapper implements Mapper {

    private Class<?> instanceClass;
    private Object instance;
    private DozerBeanMapper mapper;

    public RulesBeanMapper(Class<?> instanceClass, Object instance) {
        this.instanceClass = instanceClass;
        this.instance = instance;
        
        init();
    }

    public <T> T map(Object source, Class<T> destinationClass) {
        try {
            return mapper.map(source, destinationClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
    }

    public void map(Object source, Object destination) {
        try {
            mapper.map(source, destination);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init() {
     
        MappingProcessor mappingProcessor = new MappingProcessor(instanceClass, instance);
        Collection<Bean2BeanMappingDescriptor> mappings = mappingProcessor.loadMappings();
        
        mapper = initMapper(mappings);
    }    
    
    private DozerBeanMapper initMapper(Collection<Bean2BeanMappingDescriptor> mappings) {
        
        DozerBeanMapper mapper = new DozerBeanMapper();
        Map<String, CustomConverter> customConvertersWithId = new HashMap<String, CustomConverter>();
        
        for (Bean2BeanMappingDescriptor mapping : mappings) {
            mapper.addMapping(MappingUtils.createDozerMapping(mapping));
            customConvertersWithId.putAll(MappingUtils.getCustomConvertersMap(mapping));
        }
        
        mapper.setCustomConvertersWithId(customConvertersWithId);
        
        return mapper;
    }
      
}

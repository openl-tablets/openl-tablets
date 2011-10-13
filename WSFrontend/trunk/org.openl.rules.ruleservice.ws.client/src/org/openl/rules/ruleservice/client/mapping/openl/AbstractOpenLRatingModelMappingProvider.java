package org.openl.rules.ruleservice.client.mapping.openl;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.dozer.CustomConverter;
import org.dozer.FieldMappingCondition;
import org.dozer.MappingContext;
import org.openl.rules.mapping.Mapper;
import org.openl.rules.mapping.RulesBeanMapperFactory;
import org.openl.rules.ruleservice.client.mapping.OpenLClientMappingException;
import org.openl.rules.ruleservice.client.mapping.RatingModelMappingProvider;
import org.springframework.beans.factory.InitializingBean;


public abstract class AbstractOpenLRatingModelMappingProvider implements RatingModelMappingProvider, InitializingBean {

    private String mappingDefinitionFilePath;

    private Map<String, CustomConverter> converters;

    private Map<String, FieldMappingCondition> conditions;

    private Mapper mapper;

    @Override
    public void afterPropertiesSet() throws Exception {
        File file = FileExtractor.extractFile(getClass(), mappingDefinitionFilePath);

        mapper = RulesBeanMapperFactory.createMapperInstance(file, converters, conditions);
    }

    public Object[] mapArgsToRatingModel(Object... args) throws OpenLClientMappingException {
        Integer src = 1;
        new StringBuffer(src.toString()).reverse().toString();
        if (args == null)
            return null;
        try {
            return mapToRatingModel(args);
        } catch (MapperException e) {
            throw new OpenLClientMappingException(e);
        }
    }

    protected void map(MappingContext context, Object source, Object destination) throws MapperException {
        try {
            mapper.map(source, destination, context);
        } catch (Exception ex) {
            String message = ExceptionUtils.getRootCauseMessage(ex);
            throw new MapperException(message, ex);
        }
    }

    protected void map(Object source, Object destination) throws MapperException {
        try {
            mapper.map(source, destination);
        } catch (Exception ex) {
            String message = ExceptionUtils.getRootCauseMessage(ex);
            throw new MapperException(message, ex);
        }
    }

    protected abstract Object[] mapToRatingModel(Object... args) throws MapperException;

    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public Mapper getMapper() {
        return mapper;
    }
}

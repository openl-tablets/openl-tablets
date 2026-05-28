package org.openl.studio.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.serialization.ExtendedStdDateFormat;
import org.openl.studio.common.projection.FieldProjectionAnnotationIntrospector;
import org.openl.studio.common.projection.FieldProjectionSupport;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper(FieldProjectionSupport fieldProjectionSupport) {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        mapper.setDateFormat(new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").withLenient(false));

        // Response field projection (?fields=): tag response DTOs with a Jackson filter id so that
        // FieldProjectionResponseBodyAdvice can reduce the response to the requested fields. The no-op
        // default filter provider keeps every other serialization (and DTOs requested without ?fields)
        // byte-for-byte unchanged.
        mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(
                new FieldProjectionAnnotationIntrospector(fieldProjectionSupport),
                mapper.getSerializationConfig().getAnnotationIntrospector()));
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return mapper;
    }

}

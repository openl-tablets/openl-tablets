package org.openl.rules.spring.openapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

class OpenApiParameterServiceImplTest {

    @Test
    void unregistersModelConvertersWhenDestroyed() {
        ModelConverter converter = (type, context, chain) -> null;
        var registry = ModelConverters.getInstance();
        var initialConverterCount = registry.getConverters().size();
        var service = new OpenApiParameterServiceImpl(Optional.of(List.of(converter)),
                value -> value,
                new RequestMappingHandlerAdapter());

        try {
            assertEquals(initialConverterCount + 1, registry.getConverters().size());
        } finally {
            service.destroy();
        }
        assertEquals(initialConverterCount, registry.getConverters().size());
    }

    @Test
    void unregistersOnlyTheModelConvertersPresentAtConstruction() {
        ModelConverter registeredConverter = (type, context, chain) -> null;
        ModelConverter externalConverter = (type, context, chain) -> null;
        var converters = new ArrayList<>(List.of(registeredConverter));
        var registry = ModelConverters.getInstance();
        var initialConverterCount = registry.getConverters().size();
        var service = new OpenApiParameterServiceImpl(Optional.of(converters),
                value -> value,
                new RequestMappingHandlerAdapter());

        converters.clear();
        converters.add(externalConverter);
        registry.addConverter(externalConverter);
        try {
            service.destroy();
            assertEquals(initialConverterCount + 1, registry.getConverters().size());
            assertTrue(registry.getConverters().contains(externalConverter));
        } finally {
            registry.removeConverter(registeredConverter);
            registry.removeConverter(externalConverter);
        }
    }

    @Test
    void keepsLaterRegistrationOfTheSameConverterWhenDestroyed() {
        ModelConverter converter = (type, context, chain) -> null;
        ModelConverter interveningConverter = (type, context, chain) -> null;
        var registry = ModelConverters.getInstance();
        var service = new OpenApiParameterServiceImpl(Optional.of(List.of(converter)),
                value -> value,
                new RequestMappingHandlerAdapter());

        registry.addConverter(interveningConverter);
        registry.addConverter(converter);
        try {
            service.destroy();
            assertSame(converter, registry.getConverters().get(0));
            assertSame(interveningConverter, registry.getConverters().get(1));
        } finally {
            service.destroy();
            registry.removeConverter(converter);
            registry.removeConverter(interveningConverter);
        }
    }

    @Test
    void delegatesToTheConfiguredModelConverter() {
        var expectedSchema = new Schema<>();
        ModelConverter converter = new ModelConverter() {
            @Override
            public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
                return expectedSchema;
            }

            @Override
            public boolean isOpenapi31() {
                return true;
            }
        };
        var registry = ModelConverters.getInstance();
        var service = new OpenApiParameterServiceImpl(Optional.of(List.of(converter)),
                value -> value,
                new RequestMappingHandlerAdapter());

        try {
            var serviceOwnedConverter = registry.getConverters().getFirst();
            assertNotSame(converter, serviceOwnedConverter);
            assertSame(expectedSchema,
                    serviceOwnedConverter.resolve(new AnnotatedType(String.class), null, Collections.emptyIterator()));
            assertTrue(serviceOwnedConverter.isOpenapi31());
        } finally {
            service.destroy();
        }
    }
}

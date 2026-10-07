package org.openl.studio.common.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

/**
 * Binds a request parameter to an enum constant through Jackson, so the value accepted on the wire is
 * exactly the one the constant serializes to in JSON — its {@code @JsonProperty} code, or the name when
 * it has none.
 *
 * <p>Any enum can therefore be a controller parameter with no hand-written string mapping, while the
 * OpenAPI schema stays precise: the generator reads the same Jackson metadata and never sees this
 * binding. An unknown value raises {@link IllegalArgumentException}, which Spring reports as a
 * {@code 400}. An enum with its own dedicated {@code Converter} keeps it — a converter for a concrete
 * enum type is more specific than this factory and wins.
 */
@Component
@RequiredArgsConstructor
public class JacksonEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    private final ObjectMapper objectMapper;

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            if (source == null || source.isBlank()) {
                return null;
            }
            try {
                return objectMapper.readValue(objectMapper.writeValueAsString(source), targetType);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Unknown " + targetType.getSimpleName() + " value: " + source, e);
            }
        };
    }
}

package org.openl.rules.project.openapi;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenAPIRefResolver {
    private final Map<String, Object> resolvedByRefCache = new HashMap<>();
    private final OpenAPI openAPI;

    public OpenAPIRefResolver(OpenAPI openAPI) {
        this.openAPI = Objects.requireNonNull(openAPI, "openAPI cannot be null");
    }

    public Object resolve(String ref) {
        return resolveByRef(ref, () -> {
        });
    }

    public <T> T resolve(T obj, Function<T, String> getRefFunc) {
        return resolve(obj, getRefFunc, () -> {
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(T obj, Function<T, String> getRefFunc, Runnable retNotFoundFunc) {
        if (obj != null && getRefFunc.apply(obj) != null) {
            return resolve((T) resolveByRef(getRefFunc.apply(obj), retNotFoundFunc), getRefFunc);
        }
        return obj;
    }

    private Object resolveByRef(String ref, Runnable retNotFoundFunc) {
        if (resolvedByRefCache.containsKey(ref)) {
            return resolvedByRefCache.get(ref);
        }
        String expression = ref.substring(1);
        String[] expressionParts = expression.split("(?=/)");
        Object resolvedByRef = openAPI;
        try {
            for (String expressionPart : Arrays.stream(expressionParts)
                .map(e -> e.substring(1))
                .collect(Collectors.toList())) {
                if (resolvedByRef != null) {
                    try {
                        Field field = resolvedByRef.getClass().getDeclaredField(expressionPart);
                        field.setAccessible(true);
                        resolvedByRef = field.get(resolvedByRef);
                    } catch (NoSuchFieldException | SecurityException e) {
                        if (Map.class.isAssignableFrom(resolvedByRef.getClass())) {
                            resolvedByRef = ((Map<?, ?>) resolvedByRef).get(expressionPart);
                        } else {
                            resolvedByRef = null;
                        }
                    } catch (IllegalAccessException e) {
                        resolvedByRef = null;
                    }
                }
            }
        } catch (Exception e) {
            resolvedByRef = null;
        }
        if (resolvedByRef != openAPI && resolvedByRef != null) {
            resolvedByRefCache.put(ref, resolvedByRef);
            return resolvedByRef;
        } else {
            resolvedByRefCache.put(ref, null);
            retNotFoundFunc.run();
            return null;
        }
    }

}

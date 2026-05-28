package org.openl.studio.common.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.springframework.stereotype.Component;

import org.openl.studio.common.model.PageResponse;

/**
 * Shared logic of the REST response field projection feature.
 *
 * <p>Decides which classes are projectable response DTOs, parses the {@code ?fields=} query value into a
 * hierarchical {@link FieldNode} selection tree and resolves the root type to project for a response
 * body. All projectable DTOs share a single Jackson filter id ({@link #FILTER_ID}) so that the
 * path-aware {@link FieldProjectionPropertyFilter} can project nested objects and arrays recursively.
 * Reflection-heavy results are cached because the set of response types is bounded.
 *
 * @author Vladyslav Pikus
 */
@Component
public class FieldProjectionSupport {

    /**
     * Single Jackson filter id assigned to every projectable response DTO. A per-request filter is
     * registered under this id; when no projection is requested the no-op default filter provider keeps
     * serialization unchanged.
     */
    public static final String FILTER_ID = "openl.field-projection";

    private final FieldProjectionProperties properties;
    private final ConcurrentHashMap<Class<?>, Boolean> projectableCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JavaType, Map<String, JavaType>> propertiesCache = new ConcurrentHashMap<>();

    public FieldProjectionSupport(FieldProjectionProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isFailOnUnknownField() {
        return properties.isFailOnUnknownField();
    }

    public String getParameterName() {
        return properties.getParameterName();
    }

    /**
     * A class is projectable when it is a concrete response DTO located in one of the configured base
     * packages and does not already define an explicit {@link JsonFilter}.
     */
    public boolean isProjectable(Class<?> type) {
        if (type == null) {
            return false;
        }
        return projectableCache.computeIfAbsent(type, this::computeProjectable);
    }

    private boolean computeProjectable(Class<?> type) {
        if (type.isEnum() || type.isInterface() || type.isPrimitive() || type.isArray() || type.isAnnotation()) {
            return false;
        }
        if (type.isAnnotationPresent(JsonFilter.class)) {
            // Respect explicitly declared filters instead of overriding them.
            return false;
        }
        var pkg = type.getPackage();
        if (pkg == null) {
            return false;
        }
        var packageName = pkg.getName();
        return properties.getBasePackages().stream()
                .anyMatch(basePackage -> packageName.equals(basePackage) || packageName.startsWith(basePackage + "."));
    }

    /**
     * Resolves the type that the projection should be applied to. For collections, arrays and
     * {@link PageResponse} wrappers the element type is returned, so that only elements are reduced
     * while structural/pagination fields of the wrapper are preserved.
     *
     * @return the root type to project, or {@code null} when the body cannot/should not be projected
     */
    public Class<?> resolveTargetType(Object body) {
        return switch (body) {
            case null -> null;
            case PageResponse<?> page -> firstElementType(page.getContent());
            case Iterable<?> iterable -> firstElementType(iterable);
            case Object[] array -> firstElementType(Arrays.asList(array));
            default -> body.getClass();
        };
    }

    private static Class<?> firstElementType(Iterable<?> elements) {
        if (elements == null) {
            return null;
        }
        for (var element : elements) {
            if (element != null) {
                return element.getClass();
            }
        }
        return null;
    }

    /**
     * Parses the raw {@code fields} query value into a hierarchical selection tree.
     *
     * <p>Grammar: {@code selection := field (',' field)* } and {@code field := name ('(' selection ')')?}.
     * For example {@code id,name,modules(id,name)} selects {@code id}, {@code name} and a {@code modules}
     * object/array projected to {@code id} and {@code name}.
     */
    public FieldNode parseSelection(String raw) {
        var root = new FieldNode();
        if (raw != null) {
            parseInto(root, raw);
        }
        return root;
    }

    private static void parseInto(FieldNode parent, String selection) {
        int i = 0;
        int n = selection.length();
        while (i < n) {
            int start = i;
            while (i < n && selection.charAt(i) != ',' && selection.charAt(i) != '(') {
                i++;
            }
            var name = selection.substring(start, i).trim();
            var node = name.isEmpty() ? null : parent.getOrAdd(name);
            if (i < n && selection.charAt(i) == '(') {
                int depth = 1;
                int j = i + 1;
                while (j < n && depth > 0) {
                    char c = selection.charAt(j);
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            break;
                        }
                    }
                    j++;
                }
                if (node != null) {
                    parseInto(node, selection.substring(i + 1, j));
                }
                i = j + 1;
            }
            // Skip any remainder up to and including the next top-level comma.
            while (i < n && selection.charAt(i) != ',') {
                i++;
            }
            if (i < n) {
                i++;
            }
        }
    }

    /**
     * Validates a selection tree against the serializable properties of {@code rootType}, descending into
     * nested DTOs. Used only when {@link FieldProjectionProperties#isFailOnUnknownField()} is enabled.
     *
     * @return the dot-separated paths of requested fields that do not exist (empty if all are known)
     */
    public List<String> findUnknownPaths(ObjectMapper objectMapper, Class<?> rootType, FieldNode tree) {
        var unknown = new ArrayList<String>();
        collectUnknownPaths(objectMapper, objectMapper.constructType(rootType), tree, "", unknown);
        return unknown;
    }

    private void collectUnknownPaths(ObjectMapper objectMapper, JavaType type, FieldNode node, String prefix,
                                     List<String> unknown) {
        var properties = serializableProperties(objectMapper, type);
        for (var entry : node.children().entrySet()) {
            var name = entry.getKey();
            var propertyType = properties.get(name);
            if (propertyType == null) {
                unknown.add(prefix + name);
                continue;
            }
            var child = entry.getValue();
            if (child.hasChildren()) {
                var elementType = propertyType.isContainerType() ? propertyType.getContentType() : propertyType;
                // Only descend into nested DTOs we own; leave foreign/JDK types unvalidated (lenient).
                if (elementType != null && isProjectable(elementType.getRawClass())) {
                    collectUnknownPaths(objectMapper, elementType, child, prefix + name + ".", unknown);
                }
            }
        }
    }

    private Map<String, JavaType> serializableProperties(ObjectMapper objectMapper, JavaType type) {
        return propertiesCache.computeIfAbsent(type, t -> {
            var beanDescription = objectMapper.getSerializationConfig().introspect(t);
            var result = new LinkedHashMap<String, JavaType>();
            for (BeanPropertyDefinition property : beanDescription.findProperties()) {
                if (property.couldSerialize()) {
                    result.put(property.getName(), property.getPrimaryType());
                }
            }
            return result;
        });
    }
}

package org.openl.studio.common.projection;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.model.PageResponse;

/**
 * Shared logic for the REST response field projection feature.
 *
 * <p>Decides which classes are projectable response DTOs, parses the {@code ?fields=} query value
 * into a {@link FieldNode} selection tree, and resolves the type to project for a response body.
 *
 * <p>The feature has no runtime configuration: projection is always on, the query parameter is always
 * {@value #PARAMETER_NAME}, and unknown fields are silently dropped. All projectable DTOs share one
 * Jackson filter id ({@link #FILTER_ID}) so nested selections work recursively.
 *
 * @author Vladyslav Pikus
 */
@Component
public class FieldProjectionSupport {

    /**
     * Jackson filter id shared by every projectable response DTO.
     *
     * <p>A per-request filter is registered under it. When no projection is requested, serialization
     * is unchanged.
     */
    public static final String FILTER_ID = "openl.field-projection";

    /** Name of the query parameter clients send to opt into projection. */
    public static final String PARAMETER_NAME = "fields";

    /**
     * Hard caps on user-supplied selection input.
     *
     * <p>Generous for real use; block crafted payloads (huge strings, deep nesting, or massive token
     * counts) from driving parser work or tree growth.
     */
    static final int MAX_RAW_LENGTH = 4096;
    static final int MAX_DEPTH = 16;
    static final int MAX_NODES = 256;

    private static final String OPENL_RULES_PREFIX = "org.openl.rules.";
    private static final String OPENL_STUDIO_PREFIX = "org.openl.studio.";
    /**
     * Framework infrastructure (error responses, pagination wrappers, view markers) lives here and
     * must never be projected. Otherwise error bodies would be reduced on failed requests, and
     * pagination wrappers would be filtered instead of their content.
     */
    private static final String COMMON_MODEL_PACKAGE = "org.openl.studio.common.model";

    private final ConcurrentHashMap<Class<?>, Boolean> projectableCache = new ConcurrentHashMap<>();

    /**
     * Whether this class is a projectable response DTO.
     *
     * <p>True for concrete OpenL types under {@code org.openl.rules.*} or {@code org.openl.studio.*}.
     *
     * <p>Excluded: framework-internal types in {@value #COMMON_MODEL_PACKAGE}, and any class with an
     * explicit {@link JsonFilter} annotation.
     */
    public boolean isProjectable(Class<?> type) {
        if (type == null) {
            return false;
        }
        return projectableCache.computeIfAbsent(type, FieldProjectionSupport::computeProjectable);
    }

    private static boolean computeProjectable(Class<?> type) {
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
        if (!packageName.startsWith(OPENL_RULES_PREFIX) && !packageName.startsWith(OPENL_STUDIO_PREFIX)) {
            return false;
        }
        return !packageName.equals(COMMON_MODEL_PACKAGE) && !packageName.startsWith(COMMON_MODEL_PACKAGE + ".");
    }

    /**
     * The type to project for this response body, or {@code null} when the body cannot be projected.
     *
     * <p>For collections, arrays and {@link PageResponse} wrappers the element type is returned --
     * only the elements are reduced, while pagination metadata in the wrapper is preserved.
     */
    public Class<?> resolveTargetType(Object body) {
        return switch (body) {
            case null -> null;
            case PageResponse<?> page -> firstElementType(page.getContent());
            case Iterable<?> iterable -> firstElementType(iterable);
            case Object[] array -> firstElementType(Arrays.asList(array));
            default -> ClassUtils.getUserClass(body);
        };
    }

    private static Class<?> firstElementType(Iterable<?> elements) {
        if (elements == null) {
            return null;
        }
        for (var element : elements) {
            if (element != null) {
                // Unwrap CGLIB/AOP proxies so the proxy class doesn't shadow the projectable DTO class.
                return ClassUtils.getUserClass(element);
            }
        }
        return null;
    }

    /**
     * Parses the raw {@code fields} query value into a selection tree.
     *
     * <p>Grammar: {@code selection := field (',' field)*}, {@code field := name ('(' selection ')')?}.
     * Example: {@code id,name,modules(id,name)} selects {@code id}, {@code name}, and {@code modules}
     * projected to {@code id} and {@code name}.
     *
     * <p>Strict on real mistakes -- unmatched parentheses, or {@code '('} without a preceding name,
     * produce a {@code 400}. Forgiving on noise -- empty fields between commas and a trailing comma
     * are skipped.
     *
     * <p>Repeating a name merges selections: {@code child(a),child(b)} yields the same tree as
     * {@code child(a,b)}. The {@link #MAX_NODES} cap counts requested tokens (including duplicates),
     * not unique tree nodes, so heavily repeated input still hits the cap.
     *
     * @throws BadRequestException for malformed input or when {@link #MAX_RAW_LENGTH},
     *                             {@link #MAX_DEPTH} or {@link #MAX_NODES} is exceeded
     */
    public FieldNode parseSelection(String raw) {
        var root = new FieldNode();
        if (raw == null) {
            return root;
        }
        if (raw.length() > MAX_RAW_LENGTH) {
            throw tooLarge();
        }
        parseInto(root, raw);
        return root;
    }

    /**
     * Walks the input once and builds the selection tree as the grammar dictates:
     * <ul>
     *     <li>{@code name,} -- attach {@code name} as a child of the current parent;</li>
     *     <li>{@code name(} -- attach {@code name} and descend into it;</li>
     *     <li>{@code name)} -- attach trailing {@code name} (if any) and ascend to the enclosing parent;</li>
     *     <li>other characters -- accumulate into the current name.</li>
     * </ul>
     *
     * <p>Depth and node counts are checked against {@link #MAX_DEPTH} and {@link #MAX_NODES} as
     * parsing proceeds, so client input cannot drive unbounded recursion or tree growth.
     */
    private static void parseInto(FieldNode root, String raw) {
        var parents = new ArrayDeque<FieldNode>();
        parents.push(root);
        var nameBuffer = new StringBuilder();
        var nodeCount = new AtomicInteger();
        int depth = 0;

        for (int i = 0, len = raw.length(); i < len; i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '(' -> {
                    if (++depth > MAX_DEPTH) {
                        throw tooLarge();
                    }
                    var child = flushAndCount(parents.peek(), nameBuffer, nodeCount);
                    if (child == null) {
                        throw malformedAt("'(' must be preceded by a field name", i);
                    }
                    parents.push(child);
                }
                case ')' -> {
                    if (depth == 0) {
                        throw malformedAt("unmatched ')'", i);
                    }
                    flushAndCount(parents.peek(), nameBuffer, nodeCount);
                    parents.pop();
                    depth--;
                }
                case ',' -> flushAndCount(parents.peek(), nameBuffer, nodeCount);
                default -> nameBuffer.append(c);
            }
        }
        if (depth > 0) {
            // Don't report a character position here -- the meaningful spot is the unclosed '(',
            // which may be far from the end of input, so a single index would mislead more than help.
            throw malformed("unclosed '('");
        }
        // Trailing field has no separator after it; flush whatever is left in the buffer.
        flushAndCount(parents.peek(), nameBuffer, nodeCount);
    }

    /**
     * Attaches the buffered name (if non-blank) as a child of {@code parent} and counts it against
     * {@link #MAX_NODES}. The buffer is cleared on return.
     *
     * <p>The cap counts every non-blank token, including duplicates that just re-resolve an existing
     * node, so heavily repeated selections still hit the cap.
     *
     * @return the (possibly pre-existing) child node, or {@code null} when nothing was added
     */
    private static FieldNode flushAndCount(FieldNode parent, StringBuilder nameBuffer, AtomicInteger nodeCount) {
        var trimmed = nameBuffer.toString().trim();
        nameBuffer.setLength(0);
        if (trimmed.isEmpty()) {
            return null;
        }
        if (nodeCount.incrementAndGet() > MAX_NODES) {
            throw tooLarge();
        }
        return parent.getOrAdd(trimmed);
    }

    private static BadRequestException tooLarge() {
        return new BadRequestException("fields.too.large.message");
    }

    private static BadRequestException malformed(String reason) {
        return new BadRequestException("fields.malformed.message", new Object[]{reason});
    }

    private static BadRequestException malformedAt(String reason, int position) {
        // Positions are 1-based in the error message for human readability.
        return new BadRequestException("fields.malformed.message",
                new Object[]{reason + " at position " + (position + 1)});
    }
}

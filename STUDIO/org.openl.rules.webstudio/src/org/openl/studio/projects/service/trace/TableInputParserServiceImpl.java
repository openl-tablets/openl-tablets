package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.serialization.JsonUtils;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

/**
 * Default implementation of {@link TableInputParserService}.
 * <p>
 * Parses table input JSON by auto-detecting the format and extracting
 * method parameters and runtime context. Supports both structured format
 * (with explicit "params" and "runtimeContext" fields) and raw user input
 * (field names matching parameter names).
 * </p>
 *
 */
@Service
public class TableInputParserServiceImpl implements TableInputParserService {

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult parseInput(String inputJson, IOpenMethod method, ObjectMapper mapper) {
        if (StringUtils.isBlank(inputJson)) {
            return new ParseResult(new Object[method.getSignature().getNumberOfParameters()], null);
        }

        try {
            JsonNode rootNode = mapper.readTree(inputJson);

            // Auto-detect: if has "params" key, it's structured format
            if (rootNode.isObject() && rootNode.has("params")) {
                return parseStructuredFormat(rootNode, method, mapper);
            } else {
                return parseRawFormat(inputJson, rootNode, method, mapper);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON input: " + e.getMessage(), e);
        }
    }

    /**
     * Parses structured format: {@code {"params": {...}, "runtimeContext": {...}}}
     *
     * @param rootNode the parsed JSON root node
     * @param method   the method being executed
     * @param mapper   ObjectMapper for deserialization
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseStructuredFormat(JsonNode rootNode, IOpenMethod method, ObjectMapper mapper)
            throws IOException {
        IMethodSignature signature = method.getSignature();
        int paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];
        IRulesRuntimeContext runtimeContext = null;

        JsonNode paramsNode = rootNode.get("params");
        if (paramsNode != null && paramsNode.isObject()) {
            for (int i = 0; i < paramCount; i++) {
                String paramName = signature.getParameterName(i);
                JsonNode paramValue = paramsNode.get(paramName);
                if (paramValue != null) {
                    String paramJson = mapper.writeValueAsString(paramValue);
                    params[i] = JsonUtils.fromJSON(paramJson,
                            signature.getParameterType(i).getInstanceClass(), mapper);
                }
            }
        }

        JsonNode contextNode = rootNode.get("runtimeContext");
        if (contextNode != null && !contextNode.isNull()) {
            String contextJson = mapper.writeValueAsString(contextNode);
            runtimeContext = JsonUtils.fromJSON(contextJson, IRulesRuntimeContext.class, mapper);
        }

        return new ParseResult(params, runtimeContext);
    }

    /**
     * Parses raw user format like InputArgsBean.getParams() does.
     * <p>
     * Supports:
     * <ul>
     *   <li>{@code {"paramName": value, ...}} with optional runtime context</li>
     *   <li>Plain value for single parameter methods</li>
     * </ul>
     * </p>
     *
     * @param inputJson the original input JSON string
     * @param rootNode  the parsed JSON root node
     * @param method    the method being executed
     * @param mapper    ObjectMapper for deserialization
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseRawFormat(String inputJson, JsonNode rootNode, IOpenMethod method,
                                       ObjectMapper mapper) throws IOException {
        IMethodSignature signature = method.getSignature();
        int paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];

        // Not a JSON object - might be plain value for single parameter
        if (!rootNode.isObject()) {
            if (paramCount == 1) {
                params[0] = JsonUtils.fromJSON(inputJson,
                        signature.getParameterType(0).getInstanceClass(), mapper);
            }
            return new ParseResult(params, null);
        }

        // Split JSON into field map
        Map<String, String> fieldMap = JsonUtils.splitJSON(inputJson, mapper);

        if (fieldMap.isEmpty()) {
            return new ParseResult(params, null);
        }

        // Special case: single parameter without runtime context expected
        // Parse entire JSON as that parameter type
        if (paramCount == 1 && !fieldMap.containsKey("runtimeContext")) {
            params[0] = JsonUtils.fromJSON(inputJson,
                    signature.getParameterType(0).getInstanceClass(), mapper);
            return new ParseResult(params, null);
        }

        // Match fields by parameter name
        for (int i = 0; i < paramCount; i++) {
            String paramName = signature.getParameterName(i);
            String fieldJson = fieldMap.get(paramName);
            if (fieldJson != null) {
                params[i] = JsonUtils.fromJSON(fieldJson,
                        signature.getParameterType(i).getInstanceClass(), mapper);
                fieldMap.remove(paramName);
            }
        }

        // Check for explicit runtimeContext field first
        IRulesRuntimeContext runtimeContext = null;
        String contextJson = fieldMap.remove("runtimeContext");
        if (contextJson != null) {
            runtimeContext = JsonUtils.fromJSON(contextJson, IRulesRuntimeContext.class, mapper);
        } else if (!fieldMap.isEmpty()) {
            // Leftover field = runtime context (InputArgsBean behavior)
            contextJson = fieldMap.values().iterator().next();
            try {
                runtimeContext = JsonUtils.fromJSON(contextJson, IRulesRuntimeContext.class, mapper);
            } catch (Exception ignored) {
                // If it can't be parsed as runtime context, ignore
            }
        }

        return new ParseResult(params, runtimeContext);
    }
}

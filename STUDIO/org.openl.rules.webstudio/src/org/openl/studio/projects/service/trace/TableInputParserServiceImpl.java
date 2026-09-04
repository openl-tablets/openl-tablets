package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.utils.SpreadsheetResultBean;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

/**
 * Default implementation of {@link TableInputParserService}.
 * <p>
 * Parses table input JSON by auto-detecting the format and extracting
 * method parameters and runtime context. Supports both structured format
 * (with explicit "params" and "runtimeContext" fields) and raw user input.
 * Parameters can be matched by name or by their position in the method signature.
 * </p>
 *
 */
@Service
public class TableInputParserServiceImpl implements TableInputParserService {

    private static final String PARAMS_FIELD = "params";
    private static final String RUNTIME_CONTEXT_FIELD = "runtimeContext";

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult parseInput(String inputJson, IOpenMethod method, ObjectMapper mapper) {
        if (StringUtils.isBlank(inputJson)) {
            return new ParseResult(new Object[method.getSignature().getNumberOfParameters()], null);
        }

        try {
            var rootNode = mapper.readTree(inputJson);
            var beanClasses = new BeanClassRegistry();

            // Auto-detect: if has "params" key, it's structured format
            if (rootNode.isObject() && rootNode.has(PARAMS_FIELD)) {
                return parseStructuredFormat(rootNode, method, mapper, beanClasses);
            }
            return parseRawFormat(rootNode, method, mapper, beanClasses);
        } catch (IOException e) {
            // The input is typed or pasted by the user, so a value that does not fit the signature is a
            // client error, not a server failure.
            throw new BadRequestException("table.input.invalid.message", new Object[]{e.getMessage()});
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Object parseParameter(String json, IOpenClass parameterType, ObjectMapper mapper) throws IOException {
        return parseParameter(mapper.readTree(json), parameterType, mapper, new BeanClassRegistry());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatInput(Object @Nullable [] params,
                              @Nullable IRulesRuntimeContext runtimeContext,
                              @Nullable IOpenMethod method,
                              ObjectMapper mapper) throws IOException {
        var paramsNode = mapper.createObjectNode();
        if (method != null) {
            var signature = method.getSignature();
            for (var i = 0; i < signature.getNumberOfParameters(); i++) {
                var value = params != null && i < params.length ? params[i] : null;
                paramsNode.set(signature.getParameterName(i),
                        formatParameter(value, signature.getParameterType(i), mapper));
            }
        }

        var resultNode = mapper.createObjectNode();
        resultNode.set(PARAMS_FIELD, paramsNode);
        if (runtimeContext != null) {
            resultNode.set(RUNTIME_CONTEXT_FIELD, mapper.valueToTree(runtimeContext));
        }
        return mapper.writeValueAsString(resultNode);
    }

    /**
     * Parses structured format with named or positional parameters:
     * {@code {"params": {...}, "runtimeContext": {...}}} or {@code {"params": [...]}}.
     *
     * @param rootNode    the parsed JSON root node
     * @param method      the method being executed
     * @param mapper      ObjectMapper for deserialization
     * @param beanClasses spreadsheet bean classes of the modules met while reading this input
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseStructuredFormat(JsonNode rootNode,
                                              IOpenMethod method,
                                              ObjectMapper mapper,
                                              BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];
        IRulesRuntimeContext runtimeContext = null;

        var paramsNode = rootNode.get(PARAMS_FIELD);
        if (paramsNode != null && paramsNode.isArray()) {
            params = parsePositionalParameters(paramsNode, method, mapper, beanClasses);
        } else if (paramsNode != null && paramsNode.isObject()) {
            for (var i = 0; i < paramCount; i++) {
                var paramValue = paramsNode.get(signature.getParameterName(i));
                if (paramValue != null) {
                    params[i] = parseParameter(paramValue, signature.getParameterType(i), mapper, beanClasses);
                }
            }
        }

        var contextNode = rootNode.get(RUNTIME_CONTEXT_FIELD);
        if (contextNode != null && !contextNode.isNull()) {
            runtimeContext = mapper.treeToValue(contextNode, IRulesRuntimeContext.class);
        }

        return new ParseResult(params, runtimeContext);
    }

    /**
     * Parses raw user format like InputArgsBean.getParams() does.
     * <p>
     * Supports:
     * <ul>
     *   <li>{@code {"paramName": value, ...}} with optional runtime context</li>
     *   <li>{@code [value, ...]} matched by method signature position, unless the only parameter is itself an array</li>
     *   <li>Plain value for single parameter methods</li>
     * </ul>
     * </p>
     *
     * @param rootNode    the parsed JSON root node
     * @param method      the method being executed
     * @param mapper      ObjectMapper for deserialization
     * @param beanClasses spreadsheet bean classes of the modules met while reading this input
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseRawFormat(JsonNode rootNode,
                                       IOpenMethod method,
                                       ObjectMapper mapper,
                                       BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];

        if (rootNode.isArray() && (paramCount != 1 || !signature.getParameterType(0).isArray())) {
            return new ParseResult(parsePositionalParameters(rootNode, method, mapper, beanClasses), null);
        }

        // Not a JSON object - might be plain value for single parameter
        if (!rootNode.isObject()) {
            if (paramCount == 1) {
                params[0] = parseParameter(rootNode, signature.getParameterType(0), mapper, beanClasses);
            }
            return new ParseResult(params, null);
        }

        var fieldMap = new HashMap<String, JsonNode>();
        rootNode.properties().forEach(field -> fieldMap.put(field.getKey(), field.getValue()));

        if (fieldMap.isEmpty()) {
            return new ParseResult(params, null);
        }

        // Single-parameter raw form: the whole JSON is the parameter's value. Skip this when the input
        // is the name-wrapped form {paramName: value} (or carries a runtime context): those must be
        // matched by name below, so the value fills the parameter instead of the wrapper being
        // deserialized as the parameter type (which would yield an empty object).
        if (paramCount == 1
                && !fieldMap.containsKey(signature.getParameterName(0))
                && !fieldMap.containsKey(RUNTIME_CONTEXT_FIELD)) {
            params[0] = parseParameter(rootNode, signature.getParameterType(0), mapper, beanClasses);
            return new ParseResult(params, null);
        }

        // Match fields by parameter name
        for (var i = 0; i < paramCount; i++) {
            var paramName = signature.getParameterName(i);
            var fieldNode = fieldMap.remove(paramName);
            if (fieldNode != null) {
                params[i] = parseParameter(fieldNode, signature.getParameterType(i), mapper, beanClasses);
            }
        }

        // Check for explicit runtimeContext field first
        IRulesRuntimeContext runtimeContext = null;
        var contextNode = fieldMap.remove(RUNTIME_CONTEXT_FIELD);
        if (contextNode != null) {
            runtimeContext = mapper.treeToValue(contextNode, IRulesRuntimeContext.class);
        } else if (!fieldMap.isEmpty()) {
            // Leftover field = runtime context (InputArgsBean behavior)
            try {
                runtimeContext = mapper.treeToValue(fieldMap.values().iterator().next(), IRulesRuntimeContext.class);
            } catch (Exception ignored) {
                // If it can't be parsed as runtime context, ignore
            }
        }

        return new ParseResult(params, runtimeContext);
    }

    private Object[] parsePositionalParameters(JsonNode values,
                                               IOpenMethod method,
                                               ObjectMapper mapper,
                                               BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var params = new Object[signature.getNumberOfParameters()];
        if (values.size() > params.length) {
            throw new BadRequestException("table.input.invalid.message", new Object[]{
                    "Expected at most %d positional parameter values, but got %d."
                            .formatted(params.length, values.size())});
        }
        for (var i = 0; i < values.size(); i++) {
            params[i] = parseParameter(values.get(i), signature.getParameterType(i), mapper, beanClasses);
        }
        return params;
    }

    /**
     * Reads one parameter value out of its JSON node.
     *
     * <p>A spreadsheet result parameter carries no setters of its own, so it is read through the bean class
     * generated for it — the very type OpenL Rule Services publishes — and then turned back into a spreadsheet
     * result the rule can consume. Without that detour the value comes out empty.
     */
    @Nullable
    private Object parseParameter(JsonNode node,
                                  IOpenClass parameterType,
                                  ObjectMapper mapper,
                                  BeanClassRegistry beanClasses) throws IOException {
        var spreadsheetResult = SpreadsheetResultBean.of(parameterType);
        if (spreadsheetResult == null) {
            return mapper.treeToValue(node, parameterType.getInstanceClass());
        }
        return SpreadsheetResult.convertBeansToSpreadsheetResults(
                mapper.treeToValue(node, spreadsheetResult.beanClass()),
                beanClasses.of(spreadsheetResult.type().getModule()));
    }

    /**
     * Writes one parameter value in the shape {@link #parseParameter} reads it back.
     */
    private JsonNode formatParameter(@Nullable Object value, IOpenClass parameterType, ObjectMapper mapper)
            throws IOException {
        if (value == null) {
            return mapper.nullNode();
        }
        var spreadsheetResult = SpreadsheetResultBean.of(parameterType);
        var targetClass = spreadsheetResult != null
                ? spreadsheetResult.beanClass()
                : parameterType.getInstanceClass();
        var target = spreadsheetResult != null
                ? SpreadsheetResult.convertSpreadsheetResult(value, targetClass, parameterType,
                        namingStrategyOf(mapper))
                : value;
        // Write against the declared type so the mixins registered for it apply, then read the result back as a
        // tree: a value written through writerFor cannot be turned into a node in one step.
        return mapper.readTree(mapper.writerFor(targetClass).writeValueAsString(target));
    }

    @Nullable
    private static SpreadsheetResultBeanPropertyNamingStrategy namingStrategyOf(ObjectMapper mapper) {
        return mapper.getSerializationConfig()
                .getPropertyNamingStrategy() instanceof SpreadsheetResultBeanPropertyNamingStrategy strategy
                ? strategy
                : null;
    }

    /**
     * The spreadsheet bean classes of every module met while reading one input, indexed at most once per module.
     */
    private static final class BeanClassRegistry {

        private final Map<XlsModuleOpenClass, Map<Class<?>, CustomSpreadsheetResultOpenClass>> byModule
                = new IdentityHashMap<>();

        private Map<Class<?>, CustomSpreadsheetResultOpenClass> of(XlsModuleOpenClass module) {
            return byModule.computeIfAbsent(module, SpreadsheetResultBean::beanClassesOf);
        }
    }
}

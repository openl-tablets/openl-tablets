package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.ValidationHandler;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.utils.SpreadsheetResultBean;
import org.openl.studio.projects.service.DispatchedVersionCheck;
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
@Slf4j
@Service
public class TableInputParserServiceImpl implements TableInputParserService {

    private static final String PARAMS_FIELD = "params";

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult parseInput(String inputJson, IOpenMethod method, ObjectMapper mapper) {
        if (StringUtils.isBlank(inputJson)) {
            return new ParseResult(new Object[method.getSignature().getNumberOfParameters()], null);
        }

        try {
            var root = read(inputJson, mapper);
            var fieldMap = fieldsOf(root);
            var beanClasses = new BeanClassRegistry();

            // Auto-detect: if has "params" key, it's structured format
            if (fieldMap.containsKey(PARAMS_FIELD)) {
                return parseStructuredFormat(fieldMap, method, mapper, beanClasses);
            }
            return parseRawFormat(root, fieldMap, method, mapper, beanClasses);
        } catch (IOException e) {
            // The input is typed or pasted by the user, so a value that does not fit the signature is a
            // client error, not a server failure.
            throw new BadRequestException("table.input.invalid.message", new Object[]{e.getMessage()});
        }
    }

    @Override
    public void validateInput(ParseResult parsed, DispatchedVersionCheck.Selection selection) {
        var version = selection.version();
        if (version == null) {
            return;
        }
        var env = new SimpleRulesVM().getRuntimeEnv();
        if (parsed.runtimeContext() != null) {
            env.setContext(parsed.runtimeContext());
        }
        try {
            ValidationHandler.validateArguments(version.getSignature(), selection.module(), env, parsed.params());
        } catch (OutsideOfValidDomainException e) {
            throw new BadRequestException("table.input.domain.message",
                    new Object[]{e.getValue(), e.getVocabulary(), e.getAllowedValues()});
        } catch (RuntimeException e) {
            // A check that cannot read the input is not a fault of the input: the table runs as it would without it.
            log.warn("Vocabulary values of table '{}' were not checked.", version.getName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Object parseParameter(String json, IOpenClass parameterType, ObjectMapper mapper) throws IOException {
        return parseParameter(read(json, mapper), parameterType, mapper, new BeanClassRegistry());
    }

    private static TokenBuffer read(String json, ObjectMapper mapper) throws IOException {
        try (var parser = mapper.createParser(json)) {
            parser.nextToken();
            return copyOf(parser);
        }
    }

    private static TokenBuffer copyOf(JsonParser parser) throws IOException {
        var value = new TokenBuffer(parser);
        value.copyCurrentStructure(parser);
        return value;
    }

    private static Map<String, TokenBuffer> fieldsOf(TokenBuffer value) throws IOException {
        var fieldMap = new HashMap<String, TokenBuffer>();
        try (var parser = value.asParserOnFirstToken()) {
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                var name = parser.currentName();
                parser.nextToken();
                fieldMap.put(name, copyOf(parser));
            }
        }
        return fieldMap;
    }

    /**
     * Parses structured format with named or positional parameters:
     * {@code {"params": {...}, "runtimeContext": {...}}} or {@code {"params": [...]}}.
     *
     * @param fieldMap    the fields of the JSON root object
     * @param method      the method being executed
     * @param mapper      ObjectMapper for deserialization
     * @param beanClasses spreadsheet bean classes of the modules met while reading this input
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseStructuredFormat(Map<String, TokenBuffer> fieldMap,
                                              IOpenMethod method,
                                              ObjectMapper mapper,
                                              BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];
        IRulesRuntimeContext runtimeContext = null;

        var paramsValue = fieldMap.get(PARAMS_FIELD);
        if (paramsValue.firstToken() == JsonToken.START_ARRAY) {
            params = parsePositionalParameters(paramsValue, method, mapper, beanClasses);
        } else {
            var namedValues = fieldsOf(paramsValue);
            for (var i = 0; i < paramCount; i++) {
                var paramValue = namedValues.get(signature.getParameterName(i));
                if (paramValue != null) {
                    params[i] = parseParameter(paramValue, signature.getParameterType(i), mapper, beanClasses);
                }
            }
        }

        var contextValue = fieldMap.get(RUNTIME_CONTEXT);
        if (contextValue != null) {
            runtimeContext = mapper.readValue(contextValue.asParser(), IRulesRuntimeContext.class);
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
     * @param root        the JSON root value
     * @param fieldMap    the fields of the root value when it is a JSON object
     * @param method      the method being executed
     * @param mapper      ObjectMapper for deserialization
     * @param beanClasses spreadsheet bean classes of the modules met while reading this input
     * @return parsed result with parameters and optional context
     * @throws IOException if JSON processing fails
     */
    private ParseResult parseRawFormat(TokenBuffer root,
                                       Map<String, TokenBuffer> fieldMap,
                                       IOpenMethod method,
                                       ObjectMapper mapper,
                                       BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var paramCount = signature.getNumberOfParameters();
        Object[] params = new Object[paramCount];

        if (root.firstToken() == JsonToken.START_ARRAY
                && (paramCount != 1 || !signature.getParameterType(0).isArray())) {
            return new ParseResult(parsePositionalParameters(root, method, mapper, beanClasses), null);
        }

        // Not a JSON object - might be plain value for single parameter
        if (root.firstToken() != JsonToken.START_OBJECT) {
            if (paramCount == 1) {
                params[0] = parseParameter(root, signature.getParameterType(0), mapper, beanClasses);
            }
            return new ParseResult(params, null);
        }

        if (fieldMap.isEmpty()) {
            return new ParseResult(params, null);
        }

        // Single-parameter raw form: the whole JSON is the parameter's value. Skip this when the input
        // is the name-wrapped form {paramName: value} (or carries a runtime context): those must be
        // matched by name below, so the value fills the parameter instead of the wrapper being
        // deserialized as the parameter type (which would yield an empty object).
        if (paramCount == 1
                && !fieldMap.containsKey(signature.getParameterName(0))
                && !fieldMap.containsKey(RUNTIME_CONTEXT)) {
            params[0] = parseParameter(root, signature.getParameterType(0), mapper, beanClasses);
            return new ParseResult(params, null);
        }

        // Match fields by parameter name
        for (var i = 0; i < paramCount; i++) {
            var paramName = signature.getParameterName(i);
            var fieldValue = fieldMap.remove(paramName);
            if (fieldValue != null) {
                params[i] = parseParameter(fieldValue, signature.getParameterType(i), mapper, beanClasses);
            }
        }

        return new ParseResult(params, readRuntimeContext(fieldMap, mapper));
    }

    /**
     * Reads the runtime context from the fields left after the parameters are matched: the explicit
     * runtimeContext field, or else a leftover field that can be read as one.
     */
    private static @Nullable IRulesRuntimeContext readRuntimeContext(Map<String, TokenBuffer> fieldMap,
                                                                     ObjectMapper mapper) throws IOException {
        // Check for explicit runtimeContext field first
        IRulesRuntimeContext runtimeContext = null;
        var contextValue = fieldMap.remove(RUNTIME_CONTEXT);
        if (contextValue != null) {
            runtimeContext = mapper.readValue(contextValue.asParser(), IRulesRuntimeContext.class);
        } else if (!fieldMap.isEmpty()) {
            // Leftover field = runtime context (InputArgsBean behavior)
            try {
                runtimeContext = mapper.readValue(fieldMap.values().iterator().next().asParser(),
                        IRulesRuntimeContext.class);
            } catch (Exception ignored) {
                // If it can't be parsed as runtime context, ignore
            }
        }
        return runtimeContext;
    }

    private Object[] parsePositionalParameters(TokenBuffer array,
                                               IOpenMethod method,
                                               ObjectMapper mapper,
                                               BeanClassRegistry beanClasses) throws IOException {
        var signature = method.getSignature();
        var params = new Object[signature.getNumberOfParameters()];
        var values = new ArrayList<TokenBuffer>();
        var count = 0;
        try (var parser = array.asParserOnFirstToken()) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (count < params.length) {
                    values.add(copyOf(parser));
                } else {
                    parser.skipChildren();
                }
                count++;
            }
        }
        if (count > params.length) {
            throw new BadRequestException("table.input.invalid.message", new Object[]{
                    "Expected at most %d positional parameter values, but got %d."
                            .formatted(params.length, count)});
        }
        for (var i = 0; i < values.size(); i++) {
            params[i] = parseParameter(values.get(i), signature.getParameterType(i), mapper, beanClasses);
        }
        return params;
    }

    /**
     * Reads one parameter value out of its JSON.
     *
     * <p>A spreadsheet result parameter carries no setters of its own, so it is read through the bean class
     * generated for it — the very type OpenL Rule Services publishes — and then turned back into a spreadsheet
     * result the rule can consume. Without that detour the value comes out empty.
     *
     * <p>A number is read as it is written, the way OpenL Rule Services reads a request. A {@code BigDecimal}
     * keeps every digit and is never rounded to a {@code double}. A {@code String} takes the text of the number.
     * A fractional number of no declared type stays a {@code Double}.
     */
    @Nullable
    private Object parseParameter(TokenBuffer value,
                                  IOpenClass parameterType,
                                  ObjectMapper mapper,
                                  BeanClassRegistry beanClasses) throws IOException {
        var spreadsheetResult = SpreadsheetResultBean.of(parameterType);
        if (spreadsheetResult == null) {
            return mapper.readValue(value.asParser(), parameterType.getInstanceClass());
        }
        return SpreadsheetResult.convertBeansToSpreadsheetResults(
                mapper.readValue(value.asParser(), spreadsheetResult.beanClass()),
                beanClasses.of(spreadsheetResult.type().getModule()));
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

package org.openl.studio.projects.service.trace;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Service for parsing table input JSON for method execution.
 * <p>
 * This service auto-detects the input format and parses accordingly:
 * <ul>
 *   <li><b>Structured format:</b> {@code {"params": {...}, "runtimeContext": {...}}}, where parameters can
 *       also be a positional array</li>
 *   <li><b>Raw user input:</b> a named object, a positional array, or a plain value for one parameter. A raw array
 *       remains the plain value when that parameter itself is an array</li>
 * </ul>
 * A positional array may omit trailing parameters but cannot contain more values than the method declares.
 * </p>
 *
 */
public interface TableInputParserService {

    /**
     * Result of parsing input JSON containing method parameters and optional runtime context.
     *
     * @param params         array of parsed parameter values matching the method signature
     * @param runtimeContext optional runtime context parsed from input, may be {@code null}
     */
    record ParseResult(Object[] params, IRulesRuntimeContext runtimeContext) {}

    /**
     * Parses JSON input for method execution.
     * <p>
     * Auto-detects the input format based on JSON structure:
     * <ul>
     *   <li>If JSON object contains "params" key → structured format</li>
     *   <li>Otherwise → raw user format, with values matched by parameter name or signature position</li>
     * </ul>
     * </p>
     *
     * @param inputJson JSON string to parse; may be {@code null} or blank,
     *                  in which case an empty parameter array is returned
     * @param method    the method being executed, used to determine parameter types
     *                  and names from the method signature
     * @param mapper    ObjectMapper configured for the project with appropriate
     *                  type converters and serialization settings
     * @return parsed result containing parameters array and optional runtime context
     * @throws BadRequestException if the JSON is malformed or does not fit the method signature
     */
    ParseResult parseInput(String inputJson, IOpenMethod method, ObjectMapper mapper);

    /**
     * Reads a single parameter value from its JSON.
     *
     * <p>A spreadsheet result parameter is read in the same shape OpenL Rule Services publishes for it, so
     * the request body of a deployed service can be replayed as is. Any other parameter is read into its own
     * type.
     *
     * @param json          JSON of that one parameter value
     * @param parameterType declared type of the parameter
     * @param mapper        ObjectMapper configured for the project
     * @return the parameter value, {@code null} when the JSON is the {@code null} literal
     * @throws IOException if the JSON cannot be read as the parameter type
     */
    @Nullable
    Object parseParameter(String json, IOpenClass parameterType, ObjectMapper mapper) throws IOException;
}

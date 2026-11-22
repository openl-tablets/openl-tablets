package org.openl.studio.projects.service.trace;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.service.DispatchedVersionCheck;
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

    /** Key the structured input carries the runtime context under. A run writes the context back under it. */
    String RUNTIME_CONTEXT = "runtimeContext";

    /**
     * Result of parsing input JSON containing method parameters and optional runtime context.
     *
     * @param params         array of parsed parameter values matching the method signature
     * @param runtimeContext optional runtime context parsed from input, may be {@code null}
     */
    record ParseResult(Object[] params, IRulesRuntimeContext runtimeContext) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ParseResult(var otherParams, var otherRuntimeContext)
                    && Arrays.deepEquals(params, otherParams)
                    && Objects.equals(runtimeContext, otherRuntimeContext);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.deepHashCode(params) + Objects.hashCode(runtimeContext);
        }

        @Override
        public String toString() {
            return "ParseResult[params=%s, runtimeContext=%s]".formatted(Arrays.deepToString(params), runtimeContext);
        }
    }

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
     * Checks parsed arguments against the vocabularies of the selected table version.
     *
     * <p>Nothing is checked when the version cannot be selected before the run. A check that fails for a reason other
     * than an invalid value is skipped, and the table runs as it would without it.
     *
     * @throws BadRequestException if an argument or nested datatype field is outside its vocabulary domain
     */
    void validateInput(ParseResult parsed, DispatchedVersionCheck.Selection selection);

}

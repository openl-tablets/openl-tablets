package org.openl.studio.projects.service.trace;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.types.IOpenMethod;

/**
 * Service for parsing table input JSON for method execution.
 * <p>
 * This service auto-detects the input format and parses accordingly:
 * <ul>
 *   <li><b>Structured format:</b> {@code {"params": {...}, "runtimeContext": {...}}}</li>
 *   <li><b>Raw user input:</b> {@code {"paramName": value, ...}} or plain value for single param</li>
 * </ul>
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
     *   <li>Otherwise → raw user format (field names match parameter names)</li>
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
     * @throws IllegalArgumentException if the JSON is malformed or cannot be parsed
     */
    ParseResult parseInput(String inputJson, IOpenMethod method, ObjectMapper mapper);
}

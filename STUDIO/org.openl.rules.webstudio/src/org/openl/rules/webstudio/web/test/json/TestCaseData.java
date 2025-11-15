package org.openl.rules.webstudio.web.test.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single test case from JSON input.
 * This class is used for deserializing test case data from JSON files.
 */
public class TestCaseData {

    /**
     * Optional test case ID. If not provided, will be auto-generated.
     */
    @JsonProperty("_id_")
    private String id;

    /**
     * Optional test case description.
     */
    @JsonProperty("_description_")
    private String description;

    /**
     * Input parameters for the test case.
     * Keys are parameter names, values are parameter values.
     */
    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    /**
     * Expected result of the test case execution.
     */
    @JsonProperty("_res_")
    private Object expectedResult;

    /**
     * Optional expected error message.
     * Used when testing error conditions.
     */
    @JsonProperty("_error_")
    private String expectedError;

    /**
     * Optional runtime context properties.
     */
    @JsonProperty("_context_")
    private Map<String, Object> context;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Object getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(Object expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getExpectedError() {
        return expectedError;
    }

    public void setExpectedError(String expectedError) {
        this.expectedError = expectedError;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}

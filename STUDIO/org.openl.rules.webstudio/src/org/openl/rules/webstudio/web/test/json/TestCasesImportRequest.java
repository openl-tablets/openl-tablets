package org.openl.rules.webstudio.web.test.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for importing test cases from JSON.
 * Contains the target table information and the list of test cases to import.
 */
public class TestCasesImportRequest {

    /**
     * The name of the method/table to create tests for.
     * This should match an existing executable table in the project.
     */
    @JsonProperty("tableName")
    private String tableName;

    /**
     * Optional technical name for the test table.
     * If not provided, a default name will be generated (e.g., "{tableName}Test").
     */
    @JsonProperty("testTableName")
    private String testTableName;

    /**
     * List of test cases to import.
     */
    @JsonProperty("testCases")
    private List<TestCaseData> testCases;

    /**
     * Whether to append to an existing test table or create a new one.
     * If true, will append to existing test table if it exists.
     * If false or if test table doesn't exist, will create a new test table.
     */
    @JsonProperty("appendToExisting")
    private boolean appendToExisting = false;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTestTableName() {
        return testTableName;
    }

    public void setTestTableName(String testTableName) {
        this.testTableName = testTableName;
    }

    public List<TestCaseData> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseData> testCases) {
        this.testCases = testCases;
    }

    public boolean isAppendToExisting() {
        return appendToExisting;
    }

    public void setAppendToExisting(boolean appendToExisting) {
        this.appendToExisting = appendToExisting;
    }
}

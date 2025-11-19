package org.openl.studio.mcp.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;

import org.openl.studio.mcp.McpController;

/**
 * MCP Prompt Controller for OpenL Tablets.
 * Provides all OpenL-specific prompts via the MCP prompt protocol.
 * All prompts are loaded from markdown templates with support for variable substitution.
 */
@McpController
public class PromptController {

    private final PromptLoader promptLoader;

    public PromptController(PromptLoader promptLoader) {
        this.promptLoader = promptLoader;
    }

    /**
     * Create OpenL Table - Comprehensive guide for creating decision tables, spreadsheets, and datatypes.
     */
    @McpPrompt(name = "create_rule", description = "Create OpenL Table")
    public McpSchema.GetPromptResult createRule() {
        var resource = promptLoader.load("create_rule");
        return buildPromptResult("Create OpenL Table", resource);
    }

    /**
     * Define Datatypes and Vocabularies - Guide for creating custom datatypes and vocabularies.
     */
    @McpPrompt(name = "datatype_vocabulary", description = "Define Datatypes and Vocabularies")
    public McpSchema.GetPromptResult datatypeVocabulary() {
        var resource = promptLoader.load("datatype_vocabulary");
        return buildPromptResult("Define Datatypes and Vocabularies", resource);
    }

    /**
     * Create Test Table - Step-by-step guide for creating OpenL test tables.
     * Supports optional arguments: tableName, tableType
     */
    @McpPrompt(name = "create_test", description = "Create Test Table")
    public McpSchema.GetPromptResult createTest(@McpArg(name = "tableName", description = "Name of the table being tested") String tableName,
                                                @McpArg(name = "tableType", description = "Type of table being tested (Rules, SimpleRules, Spreadsheet, etc.") String tableType) {
        var resource = promptLoader.load("create_test", Map.of("tableName", tableName, "tableType", tableType));
        return buildPromptResult("Create Test Table", resource);
    }

    /**
     * Update Test Table - Guide for modifying existing test tables.
     * Supports optional arguments: testId, tableName
     */
    @McpPrompt(name = "update_test", description = "Update Test Table")
    public McpSchema.GetPromptResult updateTest(@McpArg(name = "testId", description = "ID of the test table to update") String testId,
                                                @McpArg(name = "tableName", description = "Name of the table being tested") String tableName) {
        var resource = promptLoader.load("update_test", Map.of("testId", testId, "tableName", tableName));
        return buildPromptResult("Update Test Table", resource);
    }

    /**
     * Append to Table - Guide for efficiently appending data to existing tables.
     * Supports optional arguments: tableId, tableType
     */
    @McpPrompt(name = "append_table", description = "Append to Table")
    public McpSchema.GetPromptResult appendTable(@McpArg(name = "tableId", description = "ID of the table to append data to") String tableId,
                                                 @McpArg(name = "tableType", description = "Type of table being appended to (Datatype, Data)") String tableType) {
        var resource = promptLoader.load("append_table", Map.of("tableId", tableId, "tableType", tableType));
        return buildPromptResult("Append to Table", resource);
    }

    /**
     * Run Tests - Test selection logic and workflow for running OpenL tests.
     * Supports optional arguments: scope, tableIds
     */
    @McpPrompt(name = "run_test", description = "Run Tests")
    public McpSchema.GetPromptResult runTest(@McpArg(name = "scope", description = "Test scope: 'single', 'multiple', or 'all'") String scope,
                                             @McpArg(name = "tableIds", description = "Comma-separated list of table IDs being tested") String tableIds) {
        var resource = promptLoader.load("run_test", Map.of("scope", scope, "tableIds", tableIds));
        return buildPromptResult("Run Tests", resource);
    }

    /**
     * Dimension Properties - Explanation of OpenL dimension properties for business versioning.
     */
    @McpPrompt(name = "dimension_properties", description = "Dimension Properties")
    public McpSchema.GetPromptResult dimensionProperties() {
        var resource = promptLoader.load("dimension_properties");
        return buildPromptResult("Dimension Properties", resource);
    }

    /**
     * Execute Rule - Guide for constructing test data and executing OpenL rules.
     * Supports optional arguments: ruleName, projectId
     */
    @McpPrompt(name = "execute_rule", description = "Execute Rule")
    public McpSchema.GetPromptResult executeRule(@McpArg(name = "ruleName", description = "Name of the rule to execute") String ruleName,
                                                 @McpArg(name = "projectId", description = "ID of the project containing the rule") String projectId) {
        var resource = promptLoader.load("execute_rule", Map.of("ruleName", ruleName, "projectId", projectId));
        return buildPromptResult("Execute Rule", resource);
    }

    /**
     * Deploy Project - OpenL deployment workflow with validation and environment selection.
     * Supports optional arguments: projectId, environment
     */
    @McpPrompt(name = "deploy_project", description = "Deploy Project")
    public McpSchema.GetPromptResult deployProject(@McpArg(name = "projectId", description = "ID of project to deploy") String projectId,
                                                   @McpArg(name = "environment", description = "Target environment: 'dev', 'test', 'staging', or 'prod'") String environment) {
        var resource = promptLoader.load("deploy_project", Map.of("projectId", projectId, "environment", environment));
        return buildPromptResult("Deploy Project", resource);
    }

    /**
     * Analyze Project Errors - OpenL error analysis workflow with pattern matching and recommendations.
     * Supports optional arguments: projectId
     */
    @McpPrompt(name = "get_project_errors", description = "Analyze Project Errors")
    public McpSchema.GetPromptResult getProjectErrors(@McpArg(name = "projectId", description = "ID of project to analyze") String projectId) {
        var resource = promptLoader.load("get_project_errors", Map.of("projectId", projectId));
        return buildPromptResult("Analyze Project Errors", resource);
    }

    /**
     * File History - Guide for viewing Git-based file version history in OpenL.
     * Supports optional arguments: filePath, projectId
     */
    @McpPrompt(name = "file_history", description = "File History")
    public McpSchema.GetPromptResult fileHistory(@McpArg(name = "filePath", description = "Path to the file (e.g., 'rules/Insurance-CA-Auto.xlsx')") String filePath,
                                                 @McpArg(name = "projectId", description = "ID of the project containing the file") String projectId) {
        var resource = promptLoader.load("file_history", Map.of("filePath", filePath, "projectId", projectId));
        return buildPromptResult("File History", resource);
    }

    /**
     * Project History - Guide for viewing project-wide Git commit history.
     * Supports optional arguments: projectId
     */
    @McpPrompt(name = "project_history", description = "Project History")
    public McpSchema.GetPromptResult projectHistory(@McpArg(name = "projectId", description = "ID of the project") String projectId) {
        var resource = promptLoader.load("project_history", Map.of("projectId", projectId));
        return buildPromptResult("Project History", resource);
    }

    /**
     * Build a prompt result from a title and content string.
     *
     * @param title   The prompt title
     * @param content The prompt content
     * @return GetPromptResult with formatted content
     */
    private McpSchema.GetPromptResult buildPromptResult(String title, String content) {
        List<McpSchema.PromptMessage> messages = new ArrayList<>();
        messages.add(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(content)));
        return new McpSchema.GetPromptResult(title, messages);
    }

}

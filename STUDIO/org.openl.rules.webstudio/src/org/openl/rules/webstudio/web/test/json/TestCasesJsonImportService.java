package org.openl.rules.webstudio.web.test.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.table.xls.builder.TestTableBuilder;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Service for importing test cases from JSON files.
 * Handles parsing JSON and adding test cases to test tables.
 */
@Service
public class TestCasesJsonImportService {

    private static final Logger LOG = LoggerFactory.getLogger(TestCasesJsonImportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse JSON from input stream to TestCasesImportRequest.
     *
     * @param inputStream JSON input stream
     * @return Parsed test cases import request
     * @throws IOException if JSON parsing fails
     */
    public TestCasesImportRequest parseJson(InputStream inputStream) throws IOException {
        return objectMapper.readValue(inputStream, TestCasesImportRequest.class);
    }

    /**
     * Import test cases from JSON request to a test table.
     *
     * @param request The test cases import request
     * @return URI of the created or modified test table
     * @throws Exception if import fails
     */
    public String importTestCases(TestCasesImportRequest request) throws Exception {
        if (request.getTestCases() == null || request.getTestCases().isEmpty()) {
            throw new IllegalArgumentException("No test cases provided in the request");
        }

        if (StringUtils.isBlank(request.getTableName())) {
            throw new IllegalArgumentException("Table name is required");
        }

        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        TableSyntaxNode targetTable = findTableByName(projectModel, request.getTableName());

        if (targetTable == null) {
            throw new IllegalArgumentException("Table not found: " + request.getTableName());
        }

        if (!targetTable.isExecutableNode()) {
            throw new IllegalArgumentException(
                    "Table '" + request.getTableName() + "' is not executable. Only executable tables can be tested.");
        }

        // Determine test table name
        String testTableName = request.getTestTableName();
        if (StringUtils.isBlank(testTableName)) {
            testTableName = TestTableBuilder.getDefaultTechnicalName(targetTable);
        }

        // Find or create the test table
        TableSyntaxNode existingTestTable = null;
        if (request.isAppendToExisting()) {
            existingTestTable = findTestTableByName(projectModel, targetTable, testTableName);
        }

        if (existingTestTable != null) {
            // Append to existing test table
            return appendTestCases(existingTestTable, targetTable, request.getTestCases());
        } else {
            // Create new test table
            return createNewTestTable(targetTable, testTableName, request.getTestCases());
        }
    }

    /**
     * Find an executable table by name.
     */
    private TableSyntaxNode findTableByName(ProjectModel projectModel, String tableName) {
        for (TableSyntaxNode node : projectModel.getTableSyntaxNodes()) {
            if (node.isExecutableNode() && node.getMember() != null && tableName.equals(
                    node.getMember().getName())) {
                return node;
            }
        }
        return null;
    }

    /**
     * Find an existing test table for a given target table.
     */
    private TableSyntaxNode findTestTableByName(ProjectModel projectModel,
                                                 TableSyntaxNode targetTable,
                                                 String testTableName) {
        String targetMethodName = targetTable.getMember().getName();
        for (TableSyntaxNode node : projectModel.getTableSyntaxNodes()) {
            if (node.getType() != null && "xls.test".equals(node.getType()) && node.getMember() != null) {
                String nodeName = node.getMember().getName();
                // Test tables have format: "{MethodName} {TestTableName}"
                if (nodeName.startsWith(targetMethodName + " ") && nodeName.endsWith(testTableName)) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Create a new test table with the provided test cases.
     */
    private String createNewTestTable(TableSyntaxNode targetTable,
                                      String testTableName,
                                      List<TestCaseData> testCases) throws CreateTableException {
        LOG.info("Creating new test table '{}' for target table '{}'",
                testTableName,
                targetTable.getMember().getName());

        // Get the sheet source from the target table
        XlsSheetSourceCodeModule sheetSource = targetTable.getXlsSheetSourceCodeModule();

        // Create test table
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sheetSource);
        TestTableBuilder builder = new TestTableBuilder(gridModel);

        // Get parameters from target table
        Map<String, String> params = TestTableBuilder.getParams(targetTable);
        String header = TestTableBuilder.getHeader(targetTable, testTableName);

        // Calculate table dimensions
        int width = params.size();
        if (width < TableBuilder.PROPERTIES_MIN_WIDTH) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }
        int height = TableBuilder.HEADER_HEIGHT + 2 + testCases.size(); // header + param rows + test cases

        // Build the table
        builder.beginTable(width, height);
        builder.writeHeader(header, null);
        builder.writeProperties(Map.of(), null); // Empty properties for now
        builder.writeParams(params, null);

        // Write test case data
        writeTestCaseRows(builder, testCases, params);

        String uri = gridModel.getRangeUri(builder.getTableRegion());
        builder.endTable();

        LOG.info("Successfully created test table at URI: {}", uri);
        return uri;
    }

    /**
     * Append test cases to an existing test table.
     */
    private String appendTestCases(TableSyntaxNode existingTestTable,
                                   TableSyntaxNode targetTable,
                                   List<TestCaseData> testCases) throws Exception {
        LOG.info("Appending {} test cases to existing test table '{}'",
                testCases.size(),
                existingTestTable.getMember().getName());

        // For now, we'll implement this by reading the existing table,
        // and adding rows to it. This is more complex and would require
        // working with the POI library to insert rows.
        // For the initial implementation, we'll throw an exception
        // and suggest creating a new table instead.
        throw new UnsupportedOperationException(
                "Appending to existing test tables is not yet supported. " + "Please create a new test table or manually merge the test cases.");
    }

    /**
     * Write test case data rows to the test table.
     */
    private void writeTestCaseRows(TestTableBuilder builder,
                                   List<TestCaseData> testCases,
                                   Map<String, String> params) {
        int currentRow = builder.getCurrentRow();

        for (int i = 0; i < testCases.size(); i++) {
            TestCaseData testCase = testCases.get(i);
            int col = 0;

            // Write each parameter value
            for (String paramName : params.keySet()) {
                Object value = null;

                // Skip special columns
                if (TestMethodHelper.EXPECTED_RESULT_NAME.equals(paramName)) {
                    value = testCase.getExpectedResult();
                } else if (TestMethodHelper.EXPECTED_ERROR.equals(paramName)) {
                    value = testCase.getExpectedError();
                } else if (TestMethodHelper.DESCRIPTION_NAME.equals(paramName)) {
                    value = testCase.getDescription();
                } else if (TestMethodHelper.CONTEXT_NAME.equals(paramName)) {
                    // Context would need special handling - for now, skip it
                    value = null;
                } else if ("_id_".equals(paramName)) {
                    value = testCase.getId();
                } else {
                    // Regular parameter
                    if (testCase.getParameters() != null) {
                        value = testCase.getParameters().get(paramName);
                    }
                }

                // Write the value to the cell
                String stringValue = value != null ? String.valueOf(value) : "";
                builder.writeCell(col, currentRow, 1, 1, stringValue);
                col++;
            }

            currentRow++;
        }
    }
}

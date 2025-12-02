package org.openl.studio.projects.service.tables.read;

import org.springframework.stereotype.Component;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.StringUtils;

/**
 * Reads {@code Test} table to {@link TestView} model.
 * <p>
 * Header format: "Test <testedTableName> <testName>"
 * Example: "Test BankLimitIndex BankLimitIndexTest"
 *
 * @author Vladyslav Pikus
 */
@Component
public class TestTableReader extends AbstractDataTableReader<TestView, TestView.Builder> {

    public TestTableReader() {
        super(TestView::builder);
    }

    @Override
    protected void initialize(TestView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var header = tsn.getHeader();
        extractHeaderInfo(header.getSourceString(), builder);
        var cellValueReader = new CellValueReader(tsn.getMetaInfoReader());

        var tableBody = tsn.getTableBody();
        if (tableBody != null) {
            readAndSetTableBody(builder, tableBody, cellValueReader);
        }
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isTestTable(table);
    }

    /**
     * Extract testedTableName from header source string.
     * Header format: "Test <testedTableName> <testName>"
     * Example: "Test BankLimitIndex BankLimitIndexTest"
     * - testedTableName = "BankLimitIndex" (second token)
     * <p>
     * Note: The table name (testName) is already extracted by parent TableReader.initialize()
     * through TableSyntaxNodeUtils.str2name(), so we don't need to set it here.
     */
    private void extractHeaderInfo(String headerSource, TestView.Builder builder) {
        if (StringUtils.isBlank(headerSource)) {
            return;
        }

        String[] tokens = headerSource.trim().split("\\s+");
        // Expected format: Test <testedTableName> [testName]
        if (tokens.length >= 2) {
            builder.testedTableName(StringUtils.trimToNull(tokens[1]));
        }
    }

}

package org.openl.studio.projects.service.tables.write;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TestAppend;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.util.StringUtils;

/**
 * Writes {@link TestView} to {@code Test} table.
 *
 * Structure:
 * - updateHeader(): Writes table header ("Test <testedTableName> <testName>")
 * - updateTableHeaders(): Inherited from AbstractDataTableWriter - writes field names (row 0) and foreign keys (row 1)
 * - updateBusinessBody(): Inherited from AbstractDataTableWriter - writes display names and data rows
 *
 * @author Vladyslav Pikus
 */
public class TestTableWriter extends AbstractDataTableWriter<TestView> {

    public TestTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateHeader(TestView tableView) {
        var header = new StringBuilder(getBusinessTableType(tableView));
        if (StringUtils.isNotBlank(tableView.testedTableName)) {
            header.append(' ').append(tableView.testedTableName);
        }
        if (StringUtils.isNotBlank(tableView.name)) {
            header.append(' ').append(tableView.name);
        }
        createOrUpdateCell(table.getGridTable(), buildCellKey(0, 0), header.toString());
        updateTableHeaders(tableView);
    }

    /**
     * Append new rows to the Test table
     */
    public void append(TestAppend testAppend) {
        appendRows(testAppend.getRows());
    }

}

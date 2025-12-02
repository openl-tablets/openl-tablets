package org.openl.studio.projects.service.tables.read;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.model.tables.VocabularyView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.types.impl.AMethod;

/**
 * Reads any OpenL table to {@link SummaryTableView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SummaryTableReader extends TableReader<SummaryTableView, SummaryTableView.Builder> {

    public SummaryTableReader() {
        super(SummaryTableView::builder);
    }

    @Override
    protected void initialize(SummaryTableView.Builder builder, IOpenLTable table) {
        super.initialize(builder, table);

        var url = table.getUriParser();
        try {
            var file = new File("").getCanonicalFile()
                    .toPath()
                    .relativize(Path.of(url.getWbPath()))
                    .resolve(url.getWbName())
                    .toString();
            builder.file(file.replace("\\", "/"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve module location", e);
        }
        builder.pos(url.getRange());

        var tsn = table.getSyntaxNode();
        var member = tsn.getMember();
        if (member instanceof AMethod) {
            initializeMethodSignature(builder, table.getSyntaxNode().getHeader().getSourceString());
        }

        if (OpenLTableUtils.isVocabularyTable(table)) {
            builder.tableType(VocabularyView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleSpreadsheet(table)) {
            builder.tableType(SimpleSpreadsheetView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleRules(table)) {
            builder.tableType(SimpleRulesView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSmartRules(table)) {
            builder.tableType(SmartRulesView.TABLE_TYPE);
        } else if (OpenLTableUtils.isTestTable(table)) {
            builder.tableType(TestView.TABLE_TYPE);
        } else if (OpenLTableUtils.isDataTable(table)) {
            builder.tableType(DataView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleLookup(table)) {
            builder.tableType(LookupView.SIMPLE_TABLE_TYPE);
        } else if (OpenLTableUtils.isSmartLookup(table)) {
            builder.tableType(LookupView.SMART_TABLE_TYPE);
        } else {
            builder.tableType(OpenLTableUtils.getTableTypeItems().get(table.getType()));
        }
    }

    private void initializeMethodSignature(SummaryTableView.Builder builder, String headerSource) {
        int pos = ExecutableTableReader.rollWhitespaces(headerSource, 0);
        int start = pos;
        pos = ExecutableTableReader.rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table type
            builder.tableType(headerSource.substring(start, pos));
        }
        pos = ExecutableTableReader.rollWhitespaces(headerSource, pos);
        start = pos;
        pos = ExecutableTableReader.rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table return type
            builder.returnType(headerSource.substring(start, pos));
        }
        pos = ExecutableTableReader.rollWhitespaces(headerSource, pos);
        builder.signature(headerSource.substring(pos));
    }
}

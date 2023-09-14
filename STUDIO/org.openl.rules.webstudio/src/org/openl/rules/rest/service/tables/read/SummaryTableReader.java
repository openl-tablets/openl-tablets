package org.openl.rules.rest.service.tables.read;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.model.tables.SimpleSpreadsheetView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.model.tables.VocabularyView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.types.impl.AMethod;
import org.springframework.stereotype.Component;

/**
 * TODO description
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
            builder.file(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve module location", e);
        }
        builder.pos(url.getRange());

        var tsn = table.getSyntaxNode();
        var member = tsn.getMember();
        if (member instanceof AMethod) {
            var methodHeader = ((AMethod) member).getHeader();
            builder.signature(MethodUtil.printSignature(methodHeader, INamedThing.REGULAR))
                    .returnType(MethodUtil.printType(methodHeader.getType()));
        }

        if (OpenLTableUtils.isVocabularyTable(table)) {
            builder.tableType(VocabularyView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleSpreadsheet(table)) {
            builder.tableType(SimpleSpreadsheetView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleRules(table)) {
            builder.tableType(SimpleRulesView.TABLE_TYPE);
        } else {
            builder.tableType(OpenLTableUtils.getTableTypeItems().get(table.getType()));
        }
    }
}

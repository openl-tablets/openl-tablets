package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.rest.model.tables.VocabularyValueView;
import org.openl.rules.rest.model.tables.VocabularyView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.rest.service.tables.write.VocabularyTableWriter;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
@Component
public class VocabularyTableReader extends EditableTableReader<VocabularyView, VocabularyView.Builder> {

    public VocabularyTableReader() {
        super(VocabularyView::builder);
    }

    @Override
    protected void initialize(VocabularyView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);
        var tsn = openLTable.getSyntaxNode();
        var table = tsn.getTableBody();
        List<VocabularyValueView> values = new ArrayList<>();
        for (int row = 0; row < table.getHeight(); row++) {
            var cell = table.getCell(0, row);
            values.add(VocabularyValueView.builder().value(cell.getObjectValue()).build());
        }
        var header = tsn.getHeader();
        builder.values(values).type(getVocabularyType(header.getSourceString()));
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isVocabularyTable(table);
    }

    private static String getVocabularyType(String header) {
        var len = header.length();
        int pos1 = StringUtils.first(header, 0, len, x -> x == VocabularyTableWriter.TYPE_OPEN);
        if (pos1 < 0) {
            return null;
        }
        int pos2 = StringUtils.first(header, pos1, len, x -> x == VocabularyTableWriter.TYPE_CLOSE);
        if (pos2 < 0) {
            return null;
        }
        if (pos1 < pos2) {
            return header.substring(pos1 + 1, pos2);
        } else {
            return null;
        }
    }
}

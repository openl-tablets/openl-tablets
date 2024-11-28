package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.rest.model.tables.DatatypeFieldView;
import org.openl.rules.rest.model.tables.DatatypeView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.rest.service.tables.write.DatatypeTableWriter;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;

/**
 * Reads {@code Datatype} table to {@link DatatypeView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class DatatypeTableReader extends EditableTableReader<DatatypeView, DatatypeView.Builder> {

    public DatatypeTableReader() {
        super(DatatypeView::builder);
    }

    @Override
    protected void initialize(DatatypeView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var metaInfoReader = tsn.getMetaInfoReader();
        var table = tsn.getTableBody();
        if (table != null) {
            List<DatatypeFieldView> fields = new ArrayList<>();
            for (int rowId = 0; rowId < table.getHeight(); rowId++) {
                var row = table.getRow(rowId);
                var fieldBuilder = DatatypeFieldView.builder()
                        .type(row.getCell(DatatypeTableWriter.TYPE_COLUMN, 0).getStringValue())
                        .name(row.getCell(DatatypeTableWriter.NAME_COLUMN, 0).getStringValue());
                if (row.getWidth() > 2) {
                    var defaultValueCell = row.getCell(DatatypeTableWriter.DEFAULT_VALUE_COLUMN, 0);
                    fieldBuilder.defaultValue(getCellValue(defaultValueCell, metaInfoReader));
                }
                fields.add(fieldBuilder.build());
            }
            builder.fields(fields);
        }
        var header = tsn.getHeader();
        builder.extendz(getExtendsType(header.getSourceString()));
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isDatatypeTable(table) && !OpenLTableUtils.isVocabularyTable(table);
    }

    private static String getExtendsType(String headerSource) {
        int pos1 = headerSource.indexOf(DatatypeTableWriter.EXTENDS_KEYWORD);
        if (pos1 < 0) {
            return null;
        }
        var extendsLen = DatatypeTableWriter.EXTENDS_KEYWORD.length();
        return StringUtils.trimToNull(headerSource.substring(pos1 + extendsLen));
    }
}

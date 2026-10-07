package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
import org.openl.studio.projects.model.tables.DatatypeLayout;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.write.DatatypeTableWriter;
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
        var cellValueReader = new CellValueReader(metaInfoReader);
        if (table != null) {
            // A datatype may title its columns, and then the first row names them instead of declaring a field.
            // Where each column sits is decided by DatatypeLayout, which the writer asks too.
            //
            // The table is read as it is written. A datatype may also be authored transposed, and the compiler
            // then turns it upright before binding; no reader or writer here follows it there, because the
            // orientation cannot be told apart for every table without compiling it, and a reader has to answer
            // the same way whether or not the module compiled.
            var columns = DatatypeLayout.of(table);
            var fields = new ArrayList<DatatypeFieldView>();
            for (var rowId = columns.firstFieldRow(); rowId < table.getHeight(); rowId++) {
                fields.add(readField(table.getRow(rowId), columns, cellValueReader));
            }
            builder.fields(fields);
        }
        var header = tsn.getHeader();
        builder.extendz(getExtendsType(header.getSourceString()));
    }

    private static DatatypeFieldView readField(ILogicalTable row,
                                               DatatypeLayout.Columns columns,
                                               CellValueReader cellValueReader) {
        var fieldBuilder = DatatypeFieldView.builder()
                .type(row.getCell(columns.type(), 0).getStringValue())
                .name(row.getCell(columns.name(), 0).getStringValue());
        for (var column : DatatypeLayout.OPTIONAL_COLUMNS) {
            // Only a column the body declares carries a value, and only where the row reaches it.
            var position = columns.at(column.title());
            if (position >= 0 && row.getWidth() > position) {
                var cell = row.getCell(position, 0);
                column.into().accept(fieldBuilder,
                        column.typedValue() ? cellValueReader.apply(cell) : cell.getStringValue());
            }
        }
        return fieldBuilder.build();
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isDatatypeTable(table) && !OpenLTableUtils.isVocabularyTable(table);
    }

    private static String getExtendsType(String headerSource) {
        var pos1 = headerSource.indexOf(DatatypeTableWriter.EXTENDS_KEYWORD);
        if (pos1 < 0) {
            return null;
        }
        var extendsLen = DatatypeTableWriter.EXTENDS_KEYWORD.length();
        return StringUtils.trimToNull(headerSource.substring(pos1 + extendsLen));
    }
}

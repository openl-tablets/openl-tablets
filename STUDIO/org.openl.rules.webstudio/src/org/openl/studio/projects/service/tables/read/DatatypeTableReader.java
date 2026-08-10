package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
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
            // Which it is, is decided by the one rule the compiler uses — see DatatypeHelper.
            //
            // The table is read as it is written. A datatype may also be authored transposed, and the compiler
            // then turns it upright before binding; no reader or writer here follows it there, because the
            // orientation cannot be told apart for every table without compiling it, and a reader has to answer
            // the same way whether or not the module compiled. Reading the compiled metadata instead would make
            // every reader depend on a successful compilation, which is a different design than this one.
            var titles = DatatypeHelper.getColumnTitlesOrder(table);
            var typeColumn = titles.getOrDefault(DatatypeHelper.TYPE_COLUMN_TITLE, DatatypeTableWriter.TYPE_COLUMN);
            var nameColumn = titles.getOrDefault(DatatypeHelper.NAME_COLUMN_TITLE, DatatypeTableWriter.NAME_COLUMN);
            // Only a body that actually has a Default column carries defaults. A titled body that names none
            // has something else in that position, which is not this field's default.
            var defaultColumn = titles.isEmpty()
                    ? DatatypeTableWriter.DEFAULT_VALUE_COLUMN
                    : titles.getOrDefault(DatatypeHelper.DEFAULT_COLUMN_TITLE, -1);
            var firstFieldRow = titles.isEmpty() ? 0 : 1;
            var fields = new ArrayList<DatatypeFieldView>();
            for (var rowId = firstFieldRow; rowId < table.getHeight(); rowId++) {
                var row = table.getRow(rowId);
                var fieldBuilder = DatatypeFieldView.builder()
                        .type(row.getCell(typeColumn, 0).getStringValue())
                        .name(row.getCell(nameColumn, 0).getStringValue());
                if (defaultColumn >= 0 && row.getWidth() > defaultColumn) {
                    fieldBuilder.defaultValue(cellValueReader.apply(row.getCell(defaultColumn, 0)));
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
        var pos1 = headerSource.indexOf(DatatypeTableWriter.EXTENDS_KEYWORD);
        if (pos1 < 0) {
            return null;
        }
        var extendsLen = DatatypeTableWriter.EXTENDS_KEYWORD.length();
        return StringUtils.trimToNull(headerSource.substring(pos1 + extendsLen));
    }
}

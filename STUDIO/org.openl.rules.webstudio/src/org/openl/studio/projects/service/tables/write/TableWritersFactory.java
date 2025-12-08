package org.openl.studio.projects.service.tables.write;

import java.util.Objects;

import org.springframework.stereotype.Component;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoWriter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.model.tables.SpreadsheetView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.model.tables.VocabularyView;

@Component
public class TableWritersFactory {

    public TableWriter<? extends TableView> getNewTableWriter(TableView tableView, XlsSheetGridModel gridModel) {
        var rect = gridModel.findEmptyRect(tableView.getWidth(), tableView.getHeight());
        var gridTable = new GridTable(rect, gridModel);
        return switch (tableView) {
            case DatatypeView ignored -> new DatatypeTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case VocabularyView ignored -> new VocabularyTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case SpreadsheetView ignored -> new SpreadsheetTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case SimpleSpreadsheetView ignored ->
                    new SimpleSpreadsheetTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case SimpleRulesView ignored -> new SimpleRulesWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case SmartRulesView ignored -> new SmartRulesWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case DataView ignored -> new DataTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case TestView ignored -> new TestTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case LookupView ignored -> new LookupWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            case RawTableView ignored -> new RawTableWriter(gridTable, EmptyMetaInfoWriter.getInstance());
            default ->
                    throw new UnsupportedOperationException("Table creation is not supported for table type: " + tableView.tableType);
        };
    }

    public TableWriter<? extends TableView> getTableWriter(IOpenLTable table, String tableType) {
        // RawTableView can be used for any table type, so check it first
        if (RawTableView.TABLE_TYPE.equals(tableType)) {
            return new RawTableWriter(table);
        }

        if (Objects.equals(XlsNodeTypes.XLS_DATATYPE.toString(), table.getType())) {
            if (VocabularyView.TABLE_TYPE.equals(tableType)) {
                return new VocabularyTableWriter(table);
            } else if (DatatypeView.TABLE_TYPE.equals(tableType)) {
                return new DatatypeTableWriter(table);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_SPREADSHEET.toString(), table.getType())) {
            if (SimpleSpreadsheetView.TABLE_TYPE.equals(tableType)) {
                return new SimpleSpreadsheetTableWriter(table);
            } else if (SpreadsheetView.TABLE_TYPE.equals(tableType)) {
                return new SpreadsheetTableWriter(table);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_DT.toString(), table.getType())) {
            if (SimpleRulesView.TABLE_TYPE.equals(tableType)) {
                return new SimpleRulesWriter(table);
            } else if (SmartRulesView.TABLE_TYPE.equals(tableType)) {
                return new SmartRulesWriter(table);
            } else if (LookupView.SMART_TABLE_TYPE.equals(tableType) || LookupView.SIMPLE_TABLE_TYPE.equals(tableType)) {
                return new LookupWriter(table);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_DATA.toString(), table.getType())) {
            if (DataView.TABLE_TYPE.equals(tableType)) {
                return new DataTableWriter(table);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_TEST_METHOD.toString(), table.getType())) {
            if (TestView.TABLE_TYPE.equals(tableType)) {
                return new TestTableWriter(table);
            }
        }
        throw new UnsupportedOperationException("Table type doesn't match writer type");
    }
}

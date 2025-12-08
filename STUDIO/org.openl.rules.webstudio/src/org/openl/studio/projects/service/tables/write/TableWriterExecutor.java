package org.openl.studio.projects.service.tables.write;

import org.springframework.stereotype.Component;

import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.DataAppend;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.DatatypeAppend;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.LookupAppend;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.model.tables.RawTableAppend;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SimpleRulesAppend;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetAppend;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SmartRulesAppend;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.model.tables.SpreadsheetView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tables.TestAppend;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.model.tables.VocabularyAppend;
import org.openl.studio.projects.model.tables.VocabularyView;

@Component
public class TableWriterExecutor {

    public void executeWrite(TableWriter<? extends TableView> writer, EditableTableView tableView) {
        switch (writer) {
            case VocabularyTableWriter vocabularyTableWriter -> vocabularyTableWriter.write((VocabularyView) tableView);
            case DatatypeTableWriter datatypeTableWriter -> datatypeTableWriter.write((DatatypeView) tableView);
            case SimpleSpreadsheetTableWriter simpleSpreadsheetTableWriter ->
                    simpleSpreadsheetTableWriter.write((SimpleSpreadsheetView) tableView);
            case SpreadsheetTableWriter spreadsheetTableWriter ->
                    spreadsheetTableWriter.write((SpreadsheetView) tableView);
            case SimpleRulesWriter simpleRulesWriter -> simpleRulesWriter.write((SimpleRulesView) tableView);
            case SmartRulesWriter smartRulesWriter -> smartRulesWriter.write((SmartRulesView) tableView);
            case LookupWriter smartLookupWriter -> smartLookupWriter.write((LookupView) tableView);
            case DataTableWriter dataTableWriter -> dataTableWriter.write((DataView) tableView);
            case TestTableWriter testTableWriter -> testTableWriter.write((TestView) tableView);
            case RawTableWriter rawTableWriter -> rawTableWriter.write((RawTableView) tableView);
            default -> throw new UnsupportedOperationException("Unsupported writer: " + writer);
        }
    }

    public void executeAppend(TableWriter<? extends TableView> writer, AppendTableView tableView) {
        switch (writer) {
            case VocabularyTableWriter vocabularyTableWriter ->
                    vocabularyTableWriter.append((VocabularyAppend) tableView);
            case DatatypeTableWriter datatypeTableWriter -> datatypeTableWriter.append((DatatypeAppend) tableView);
            case SimpleSpreadsheetTableWriter simpleSpreadsheetTableWriter ->
                    simpleSpreadsheetTableWriter.append((SimpleSpreadsheetAppend) tableView);
            case SimpleRulesWriter simpleRulesWriter -> simpleRulesWriter.append((SimpleRulesAppend) tableView);
            case SmartRulesWriter smartRulesWriter -> smartRulesWriter.append((SmartRulesAppend) tableView);
            case LookupWriter smartLookupWriter -> smartLookupWriter.append((LookupAppend) tableView);
            case DataTableWriter dataTableWriter -> dataTableWriter.append((DataAppend) tableView);
            case TestTableWriter testTableWriter -> testTableWriter.append((TestAppend) tableView);
            case RawTableWriter rawTableWriter -> rawTableWriter.append((RawTableAppend) tableView);
            default -> throw new UnsupportedOperationException("Unsupported writer: " + writer);
        }
    }

}

package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Interface for update/create request models, used for request deserialization
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tableType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DatatypeView.class, name = DatatypeView.TABLE_TYPE),
        @JsonSubTypes.Type(value = VocabularyView.class, name = VocabularyView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SpreadsheetView.class, name = SpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleSpreadsheetView.class, name = SimpleSpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleRulesView.class, name = SimpleRulesView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SmartRulesView.class, name = SmartRulesView.TABLE_TYPE),
        @JsonSubTypes.Type(value = LookupView.class, names = {LookupView.SMART_TABLE_TYPE, LookupView.SIMPLE_TABLE_TYPE}),
        @JsonSubTypes.Type(value = DataView.class, name = DataView.TABLE_TYPE),
        @JsonSubTypes.Type(value = TestView.class, name = TestView.TABLE_TYPE),
        @JsonSubTypes.Type(value = RawTableView.class, name = RawTableView.TABLE_TYPE)
})
public interface EditableTableView {

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'Spreadsheet', 'SimpleSpreadsheet', 'SimpleRules', 'SmartRules', 'Lookup', 'Data', 'Test', 'RawTable')")
    String getTableType();

}

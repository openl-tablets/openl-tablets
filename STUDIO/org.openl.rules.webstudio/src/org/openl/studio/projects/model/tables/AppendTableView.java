package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Interface for append lines request models, used for request deserialization
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tableType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DatatypeAppend.class, name = DatatypeView.TABLE_TYPE),
        @JsonSubTypes.Type(value = VocabularyAppend.class, name = VocabularyView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleSpreadsheetAppend.class, name = SimpleSpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleRulesAppend.class, name = SimpleRulesView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SmartRulesAppend.class, name = SmartRulesView.TABLE_TYPE),
        @JsonSubTypes.Type(value = LookupAppend.class, names = {LookupView.SMART_TABLE_TYPE, LookupView.SIMPLE_TABLE_TYPE}),
        @JsonSubTypes.Type(value = DataAppend.class, name = DataView.TABLE_TYPE),
        @JsonSubTypes.Type(value = TestAppend.class, name = TestView.TABLE_TYPE),
        @JsonSubTypes.Type(value = RawTableAppend.class, name = RawTableView.TABLE_TYPE)
})
public interface AppendTableView {

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'SimpleSpreadsheet', 'SimpleRules', 'SmartRules', 'Lookup', 'Data', 'Test', 'RawTable')")
    String getTableType();

}

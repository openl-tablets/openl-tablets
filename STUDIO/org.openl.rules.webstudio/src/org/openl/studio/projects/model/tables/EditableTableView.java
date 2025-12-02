package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
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
@Schema(
        oneOf = {
                DatatypeView.class,
                VocabularyView.class,
                SpreadsheetView.class,
                SimpleSpreadsheetView.class,
                SimpleRulesView.class,
                SmartRulesView.class,
                LookupView.class,
                DataView.class,
                TestView.class,
                RawTableView.class
        },
        discriminatorMapping = {
                @DiscriminatorMapping(value = DatatypeView.TABLE_TYPE, schema = DatatypeView.class),
                @DiscriminatorMapping(value = VocabularyView.TABLE_TYPE, schema = VocabularyView.class),
                @DiscriminatorMapping(value = SpreadsheetView.TABLE_TYPE, schema = SpreadsheetView.class),
                @DiscriminatorMapping(value = SimpleSpreadsheetView.TABLE_TYPE, schema = SimpleSpreadsheetView.class),
                @DiscriminatorMapping(value = SimpleRulesView.TABLE_TYPE, schema = SimpleRulesView.class),
                @DiscriminatorMapping(value = SmartRulesView.TABLE_TYPE, schema = SmartRulesView.class),
                @DiscriminatorMapping(value = LookupView.SMART_TABLE_TYPE, schema = LookupView.class),
                @DiscriminatorMapping(value = LookupView.SIMPLE_TABLE_TYPE, schema = LookupView.class),
                @DiscriminatorMapping(value = DataView.TABLE_TYPE, schema = DataView.class),
                @DiscriminatorMapping(value = TestView.TABLE_TYPE, schema = TestView.class),
                @DiscriminatorMapping(value = RawTableView.TABLE_TYPE, schema = RawTableView.class)
        }
)
public interface EditableTableView {

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'Spreadsheet', 'SimpleSpreadsheet', 'SimpleRules', 'SmartRules', 'Lookup', 'Data', 'Test', 'RawTable')")
    String getTableType();

}

package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
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
@Schema(
        oneOf = {
                DatatypeAppend.class,
                VocabularyAppend.class,
                SimpleSpreadsheetAppend.class,
                SimpleRulesAppend.class,
                SmartRulesAppend.class,
                LookupAppend.class,
                DataAppend.class,
                TestAppend.class,
                RawTableAppend.class
        },
        discriminatorMapping = {
                @DiscriminatorMapping(value = DatatypeView.TABLE_TYPE, schema = DatatypeAppend.class),
                @DiscriminatorMapping(value = VocabularyView.TABLE_TYPE, schema = VocabularyAppend.class),
                @DiscriminatorMapping(value = SimpleSpreadsheetView.TABLE_TYPE, schema = SimpleSpreadsheetAppend.class),
                @DiscriminatorMapping(value = SimpleRulesView.TABLE_TYPE, schema = SimpleRulesAppend.class),
                @DiscriminatorMapping(value = SmartRulesView.TABLE_TYPE, schema = SmartRulesAppend.class),
                @DiscriminatorMapping(value = LookupView.SMART_TABLE_TYPE, schema = LookupAppend.class),
                @DiscriminatorMapping(value = LookupView.SIMPLE_TABLE_TYPE, schema = LookupAppend.class),
                @DiscriminatorMapping(value = DataView.TABLE_TYPE, schema = DataAppend.class),
                @DiscriminatorMapping(value = TestView.TABLE_TYPE, schema = TestAppend.class),
                @DiscriminatorMapping(value = RawTableView.TABLE_TYPE, schema = RawTableAppend.class)
        }
)
public interface AppendTableView {

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'SimpleSpreadsheet', 'SimpleRules', 'SmartRules', 'Lookup', 'Data', 'Test', 'RawTable')")
    String getTableType();

}

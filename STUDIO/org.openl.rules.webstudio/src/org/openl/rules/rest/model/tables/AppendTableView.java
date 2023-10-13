package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Interface for append lines request models, used for request deserialization
 * 
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tableType", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = DatatypeAppend.class, name = DatatypeView.TABLE_TYPE),
        @JsonSubTypes.Type(value = VocabularyAppend.class, name = VocabularyView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleSpreadsheetAppend.class, name = SimpleSpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleRulesAppend.class, name = SimpleRulesView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SmartRulesAppend.class, name = SmartRulesView.TABLE_TYPE) })
@Schema(oneOf = { DatatypeAppend.class,
        VocabularyAppend.class,
        SimpleSpreadsheetAppend.class,
        SimpleRulesAppend.class,
        SmartRulesAppend.class })
public interface AppendTableView {

    @JsonIgnore
    String getTableType();

}

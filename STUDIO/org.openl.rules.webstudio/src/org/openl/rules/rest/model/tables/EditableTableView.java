package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Interface for update/create request models, used for request deserialization
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tableType", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = DatatypeView.class, name = DatatypeView.TABLE_TYPE),
        @JsonSubTypes.Type(value = VocabularyView.class, name = VocabularyView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SpreadsheetView.class, name = SpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleSpreadsheetView.class, name = SimpleSpreadsheetView.TABLE_TYPE),
        @JsonSubTypes.Type(value = SimpleRulesView.class, name = SimpleRulesView.TABLE_TYPE) })
@Schema(oneOf = { DatatypeView.class,
        VocabularyView.class,
        SpreadsheetView.class,
        SimpleSpreadsheetView.class,
        SimpleRulesView.class })
public interface EditableTableView {

    @JsonIgnore
    String getTableType();

}

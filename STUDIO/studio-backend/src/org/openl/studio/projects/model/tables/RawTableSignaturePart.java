package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A part of the header text of a table.
 *
 * @author Yury Molchan
 */
@Schema(description = "A part of the header text of a table")
public enum RawTableSignaturePart {

    /** The keyword, such as {@code SmartRules} or {@code Datatype}. */
    @JsonProperty("keyword")
    KEYWORD,

    /**
     * The type the header names besides the table: the type a rule returns, the parent of a Datatype, the type of the
     * values of a Vocabulary, the type of the rows of a Data table or the table a Test tests.
     */
    @JsonProperty("type")
    TYPE,

    /** The name of the table. */
    @JsonProperty("name")
    NAME,

    /** The parentheses and the commas of the parameters. */
    @JsonProperty("punctuation")
    PUNCTUATION,

    /** The type of a parameter. */
    @JsonProperty("parameterType")
    PARAMETER_TYPE,

    /** The name of a parameter. */
    @JsonProperty("parameterName")
    PARAMETER_NAME
}

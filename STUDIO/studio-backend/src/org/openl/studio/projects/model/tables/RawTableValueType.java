package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The type of a value of the body of a table, as the compiler reads it: a screen colours a value by it.
 *
 * @author Yury Molchan
 */
@Schema(description = "The type of a value of the body of a table, as the compiler reads it")
public enum RawTableValueType {

    /** An OpenL expression: a cell starting with {@code =}, the body of a Method table, a TBasic operation. */
    @JsonProperty("code")
    CODE,

    /** A number, or a range of numbers. */
    @JsonProperty("number")
    NUMBER,

    /** A date, or a range of dates. */
    @JsonProperty("date")
    DATE,

    /** {@code true} or {@code false}. */
    @JsonProperty("boolean")
    BOOLEAN,

    /** Any other value; an array takes the type of its elements. */
    @JsonProperty("string")
    STRING
}

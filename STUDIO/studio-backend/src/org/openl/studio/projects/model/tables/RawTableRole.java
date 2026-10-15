package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a cell of a table means, which the cell takes its look from.
 *
 * <p>A role is a meaning, never a position: it is read from the compiled table. Every table shares the same roles,
 * each kind holding them in its own structure.
 *
 * @author Yury Molchan
 */
@Schema(description = "What a cell of a table means, which the cell takes its look from")
public enum RawTableRole {

    /** The header cell: the keyword, the name, the type and the parameters of the table. */
    @JsonProperty("signature")
    SIGNATURE,

    /** A table property, its name and its value. */
    @JsonProperty("properties")
    PROPERTIES,

    /** A technical declaration the compiler reads, such as {@code C1}, {@code RET1} or the field names of a Data table. */
    @JsonProperty("code")
    CODE,

    /** A title of what the table takes, such as the title of a condition. */
    @JsonProperty("inputTitle")
    INPUT_TITLE,

    /** A title of what the table gives, such as the title of a returned value or of a Spreadsheet column. */
    @JsonProperty("outputTitle")
    OUTPUT_TITLE,

    /** What names a row or a column of the body, such as a Data ID, a rule name, a step name or a field name. */
    @JsonProperty("key")
    KEY,

    /** A value the table takes or holds, such as a condition value or a data value. */
    @JsonProperty("value")
    VALUE,

    /** A value the table gives, such as a returned value or an expected result. */
    @JsonProperty("output")
    OUTPUT,

    /** A heading inside the body, such as a merged Spreadsheet row or a TBasic subroutine. */
    @JsonProperty("section")
    SECTION,

    /** What the table returns, such as the {@code RETURN} step or the steps of a Spreadsheet marked {@code *}. */
    @JsonProperty("result")
    RESULT
}

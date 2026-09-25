package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * How the cells of a table that can only be read as the cells it holds are written.
 *
 * <p>A Spreadsheet, a Datatype, a table written as a grid — its cells are told one by one, because nothing it
 * declares says what a cell nobody has written in yet would hold.
 *
 * @author Vladyslav Pikus
 */
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "The editors the cells of a table hold, told one cell at a time")
public class RawTableEditorsView extends TableEditorsView {

    public static final String KIND = "raw";

    @Override
    public String getKind() {
        return KIND;
    }
}

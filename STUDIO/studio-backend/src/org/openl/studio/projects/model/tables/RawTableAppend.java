package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for appending rows to raw tables.
 * <p>
 * Represents rows to append to the end of a table in raw 2D matrix format.
 * Each row is a list of RawTableCell objects.
 *
 * @author Vladyslav Pikus
 */
public class RawTableAppend implements AppendTableView {

    @Getter
    @Schema(description = "Rows to append as a 2D matrix of raw table cells")
    @Setter
    @NotEmpty
    private List<List<@Valid RawTableCell>> rows;

    @Override
    public String getTableType() {
        return RawTableView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }

}

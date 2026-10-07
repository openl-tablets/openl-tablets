package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The edits to apply to a table's raw source as one change.
 *
 * <p>The edits are applied in the order they are given, each seeing the table as the previous one left it: a row
 * inserted by an earlier edit shifts the coordinates every later edit addresses.
 *
 * <p>The table is written once, after the last edit. An edit that is refused ends the sequence and nothing of it
 * reaches the table, so an editing session is sent as a single request rather than one request per action.
 *
 * @param actions the edits to apply, in order
 * @author Vladyslav Pikus
 */
public record RawTableSourceActions(
        @Parameter(description = "Edits to apply, in order. Each one addresses the table as the previous one left "
                + "it, so an insert or a delete shifts the coordinates of everything that follows.")
        @NotEmpty
        @Valid
        List<RawTableSourceAction> actions
) {
}

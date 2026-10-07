package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.ParameterValue;

/**
 * One case of a test table, as it is written in the table.
 *
 * <p>The parameters carry the values of the case. The runtime context columns come first, then the input columns
 * of the tested rule. Each parameter is named by its column and described by the column's display name.
 *
 * <p>In the list of cases a value with inner structure is marked lazy and left out. A case read on its own
 * carries every value.
 *
 * @param id          id of the case, from its {@code _id_} column or its position in the table
 * @param description description of the case, absent when the table has no description column
 * @param parameters  values of the case
 */
@Builder
@Schema(description = "One case of a test table")
public record TestCaseView(
        @Parameter(description = "Id of the case, from its _id_ column or its position in the table")
        String id,

        @Parameter(description = "Description of the case, absent when the table has no description column")
        @Nullable String description,

        @Parameter(description = """
                Values of the case: the runtime context columns first, then the input columns of the tested \
                rule""")
        List<ParameterValue> parameters
) {
}

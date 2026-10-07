package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * What a table says about itself besides its cells: its name and the properties that apply to it.
 *
 * <p>The properties are the ones the Table Details panel has always shown — those written on the table and
 * those it inherits from the properties table of its module or category. They arrive grouped and in the order
 * the property dictionary declares them, so a screen draws them without deciding anything.
 *
 * <p>The answer also says whether the properties may be written at all and which of them may still be added, so
 * the panel offers exactly what this kind of table accepts.
 *
 * @param name               name of the table, as its header spells it
 * @param kind               the family the table belongs to, which decides the properties it may declare
 * @param groups             properties that apply to the table, by the group they belong to
 * @param canEditProperties  whether properties may be written on this table
 * @param available          names of the properties that may still be written on it
 * @author Vladyslav Pikus
 */
@Builder
public record TableDetailsView(
        @Parameter(description = "Name of the table")
        String name,

        @Parameter(description = "Kind of the table object, which decides the properties it may be given")
        TableKind kind,

        @Parameter(description = """
                Properties that apply to the table, grouped as the property dictionary groups them. \
                Empty for a kind of table that carries no properties.""")
        List<TablePropertyGroupView> groups,

        @Parameter(description = """
                Whether properties may be written on this table. A table that carries no properties at all — \
                the environment, a properties table, whatever OpenL could not name — accepts none.""")
        boolean canEditProperties,

        @Parameter(description = """
                Names of the properties that may still be written on the table: the ones its kind accepts, \
                less the ones it already carries or inherits. A value already shown is changed where it \
                stands, which writes it onto the table.""")
        List<String> available
) {
}

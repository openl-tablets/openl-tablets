package org.openl.rules.lang.xls.types.meta;

import java.util.List;

import org.openl.rules.datatype.binding.AliasDatatypeBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.properties.PropertiesHelper;

public class AliasDatatypeMetaInfoReader extends BaseMetaInfoReader<AliasDatatypeBoundNode> {
    public AliasDatatypeMetaInfoReader(AliasDatatypeBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        return null;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        var baseClass = getBoundNode().getDomainOpenClass().getBaseClass();
        var isArray = baseClass.isArray();
        if (isArray) {
            baseClass = baseClass.getAggregateInfo().getComponentType(baseClass);
        }
        return new CellMetaInfo(baseClass, isArray);
    }

    /**
     * The values of the vocabulary: every cell under its header and its properties, on past the table's edge.
     *
     * <p>Each value holds the type the header names, so a value written into a line laid down later holds it
     * too, and so does the first value of a vocabulary that lists none yet.
     */
    @Override
    public List<TableArea> getAreas() {
        var table = getTableSyntaxNode().getTable();
        // The rule the binder reads the values by (DatatypeHelper.getNormalizedDataPartTable).
        var titles = PropertiesHelper.getPropertiesTableSection(table) == null ? 1 : 2;
        var valuesFrom = 0;
        for (var row = 0; row < titles; row++) {
            valuesFrom += table.getRowHeight(row);
        }
        return List.of(new TableArea(valuesFrom,
                0,
                TableArea.TO_THE_END,
                TableArea.TO_THE_END,
                getBodyMetaInfo(valuesFrom, 0)));
    }
}

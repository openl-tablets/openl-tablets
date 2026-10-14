package org.openl.rules.lang.xls.types.meta;

import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getCellSource;

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ParserUtils;

/**
 * Provides metadata for cells represented by the bound Datatype logical table.
 *
 * <p>The type and the default value of a row are described by the field the row declares, whatever its name cell
 * adds to the name, such as a runtime context property. A row that declares no field has no metadata, even when it
 * names a field declared elsewhere.
 *
 * <p>Cells outside the bound logical body have no Datatype metadata. This can occur when the raw grid grows before
 * the table is rebound.
 */
@Slf4j
public class DatatypeTableMetaInfoReader extends BaseMetaInfoReader<DatatypeTableBoundNode> {

    public DatatypeTableMetaInfoReader(DatatypeTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        var dataType = getBoundNode().getDataType();
        var identifier = getBoundNode().getParentClassIdentifier();
        if (identifier != null && dataType.getSuperClass() != null) {
            return createMetaInfo(identifier, dataType.getSuperClass().getMetaInfo());
        }
        return null;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        var logicalTable = getBoundNode().getTable();

        var firstCell = logicalTable.getCell(0, 0);
        var r = row - firstCell.getAbsoluteRow();
        var c = col - firstCell.getAbsoluteColumn();
        if (r < 0 || c < 0) {
            return getHeaderMetaInfo();
        }
        if (!logicalTable.isNormalOrientation()) {
            var temp = r;
            r = c;
            c = temp;
        }
        if (r >= logicalTable.getHeight()) {
            return null;
        }
        var field = getBoundNode().getFieldOfRow(r);
        if (field == null) {
            return null;
        }

        if (Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.DEFAULT_COLUMN_TITLE), c) || Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.EXAMPLE_COLUMN_TITLE), c)) {
            return getDefaultValueMetaInfo(field);
        } else if (Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.TYPE_COLUMN_TITLE), c)) {
            return getTypeMetaInfo(logicalTable.getRow(r), c, field);
        }
        return null;
    }

    private static CellMetaInfo getDefaultValueMetaInfo(IOpenField field) {
        var type = field.getType();
        if (type.getAggregateInfo().isAggregate(type)) {
            return new CellMetaInfo(type.getAggregateInfo().getComponentType(type), true);
        }
        return new CellMetaInfo(type, false);
    }

    private static @Nullable CellMetaInfo getTypeMetaInfo(ILogicalTable logicalRow, int c, IOpenField field) {
        GridCellSourceCodeModule typeCellSource = getCellSource(logicalRow, null, c);
        if (ParserUtils.isBlankOrCommented(typeCellSource.getCode())) {
            return null;
        }
        try {
            IdentifierNode[] idn = Tokenizer.tokenize(typeCellSource, "[]\n\r");
            return createMetaInfo(idn[0], field.getType().getMetaInfo());
        } catch (OpenLCompilationException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private static CellMetaInfo createMetaInfo(IdentifierNode identifier, IMetaInfo typeMeta) {
        if (typeMeta == null) {
            return null;
        }
        var nodeUsage = new SimpleNodeUsage(identifier,
                typeMeta.getDisplayName(INamedThing.SHORT),
                typeMeta.getSourceUrl(),
                NodeType.DATATYPE);

        return new CellMetaInfo(JavaOpenClass.STRING, false, List.of(nodeUsage));
    }
}

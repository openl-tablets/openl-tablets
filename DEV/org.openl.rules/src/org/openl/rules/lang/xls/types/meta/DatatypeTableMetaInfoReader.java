package org.openl.rules.lang.xls.types.meta;

import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getCellSource;

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

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

        if (Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.DEFAULT_COLUMN_TITLE), c) || Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.EXAMPLE_COLUMN_TITLE), c)) {
            // Default Values
            try {
                var logicalRow = logicalTable.getRow(r);
                var field = getField(logicalRow);
                if (field == null) {
                    return null;
                }
                var type = field.getType();
                var multiValue = false;
                if (type.getAggregateInfo().isAggregate(type)) {
                    type = type.getAggregateInfo().getComponentType(type);
                    multiValue = true;
                }

                return new CellMetaInfo(type, multiValue);
            } catch (OpenLCompilationException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        } else if (Objects.equals(getBoundNode().getColumnTitlesOrder().get(DatatypeHelper.TYPE_COLUMN_TITLE), c)) {
            var logicalRow = logicalTable.getRow(r);
            GridCellSourceCodeModule typeCellSource = getCellSource(logicalRow, null, c);
            if (!ParserUtils.isBlankOrCommented(typeCellSource.getCode())) {
                try {
                    var field = getField(logicalRow);
                    if (field == null) {
                        return null;
                    }
                    var fieldMetaInfo = field.getType().getMetaInfo();
                    IdentifierNode[] idn = Tokenizer.tokenize(typeCellSource, "[]\n\r");
                    return createMetaInfo(idn[0], fieldMetaInfo);
                } catch (OpenLCompilationException e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
        }
        return null;
    }

    private IOpenField getField(ILogicalTable logicalRow) throws OpenLCompilationException {
        String fieldName = getName(logicalRow);
        if (fieldName == null) {
            return null;
        }

        var dataType = getBoundNode().getDataType();
        var field = dataType.getField(fieldName);
        if (field == null) {
            return null;
        }
        return field;
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

    private static String getName(ILogicalTable row) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, null, 1);
        IdentifierNode[] idn = Tokenizer.tokenize(nameCellSource, " \r\n");
        if (idn.length != 1) {
            // Table with error. Skip it
            return null;
        } else {
            var name = idn[0].getIdentifier();
            if (name.endsWith(DatatypeTableBoundNode.TRANSIENT_FIELD_SUFFIX) || name
                    .endsWith(DatatypeTableBoundNode.NON_TRANSIENT_FIELD_SUFFIX)) {
                return name.substring(0, name.length() - 1);
            }
            return name;
        }
    }
}

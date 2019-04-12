package org.openl.rules.lang.xls.types.meta;

import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getCellSource;
import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getIdentifierNode;

import java.util.Collections;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatatypeTableMetaInfoReader extends BaseMetaInfoReader<DatatypeTableBoundNode> {
    private final Logger log = LoggerFactory.getLogger(DatatypeTableMetaInfoReader.class);

    public DatatypeTableMetaInfoReader(DatatypeTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        DatatypeOpenClass dataType = getBoundNode().getDataType();
        IdentifierNode identifier = getBoundNode().getParentClassIdentifier();
        if (identifier != null && dataType.getSuperClass() != null) {
            return createMetaInfo(identifier, dataType.getSuperClass().getMetaInfo());
        }
        return null;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        ILogicalTable logicalTable = getBoundNode().getTable();

        ICell firstCell = logicalTable.getCell(0, 0);
        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();
        if (!logicalTable.isNormalOrientation()) {
            int temp = r;
            r = c;
            c = temp;
        }
        if (c > 0) {
            if (c == 2) {
                // Default Values
                try {
                    ILogicalTable logicalRow = logicalTable.getRow(r);
                    IOpenField field = getField(logicalRow);
                    if (field == null) {
                        return null;
                    }
                    IOpenClass type = field.getType();
                    boolean multiValue = false;
                    if (type.getAggregateInfo().isAggregate(type)) {
                        type = type.getAggregateInfo().getComponentType(type);
                        multiValue = true;
                    }

                    return new CellMetaInfo(type, multiValue);
                } catch (OpenLCompilationException e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }

            // Field names
            return null;
        }

        ILogicalTable logicalRow = logicalTable.getRow(r);
        GridCellSourceCodeModule typeCellSource = getCellSource(logicalRow, null, 0);
        if (!ParserUtils.isBlankOrCommented(typeCellSource.getCode())) {
            try {
                IOpenField field = getField(logicalRow);
                if (field == null) {
                    return null;
                }
                IMetaInfo fieldMetaInfo = field.getType().getMetaInfo();
                IdentifierNode[] idn = Tokenizer.tokenize(typeCellSource, "[]\n\r");
                return createMetaInfo(idn[0], fieldMetaInfo);
            } catch (OpenLCompilationException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        return null;
    }

    private IOpenField getField(ILogicalTable logicalRow) throws OpenLCompilationException {
        String fieldName = getName(logicalRow);
        if (fieldName == null) {
            return null;
        }

        DatatypeOpenClass dataType = getBoundNode().getDataType();
        IOpenField field = dataType.getField(fieldName);
        if (field == null) {
            return null;
        }
        return field;
    }

    private CellMetaInfo createMetaInfo(IdentifierNode identifier, IMetaInfo typeMeta) {
        if (typeMeta == null) {
            return null;
        }
        SimpleNodeUsage nodeUsage = new SimpleNodeUsage(identifier,
            typeMeta.getDisplayName(INamedThing.SHORT),
            typeMeta.getSourceUrl(),
            NodeType.DATATYPE);

        return new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
    }

    private String getName(ILogicalTable row) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, null, 1);
        IdentifierNode[] idn = getIdentifierNode(nameCellSource);
        if (idn.length != 1) {
            // Table with error. Skip it
            return null;
        } else {
            return idn[0].getIdentifier();
        }
    }
}

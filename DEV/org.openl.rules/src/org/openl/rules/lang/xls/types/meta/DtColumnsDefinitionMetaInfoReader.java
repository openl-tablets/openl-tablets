package org.openl.rules.lang.xls.types.meta;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.dt.ADtColumnsDefinitionTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CellKey.CellKeyFactory;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public class DtColumnsDefinitionMetaInfoReader extends BaseMetaInfoReader<ADtColumnsDefinitionTableBoundNode> {

    private final Map<CellKey, Pair<CompositeMethod, String>> expressions = new HashMap<>();
    private final Map<CellKey, Triple<IOpenMethodHeader, String, Integer>> inputs = new HashMap<>();
    private final Map<CellKey, Pair<IParameterDeclaration, String>> parameters = new HashMap<>();

    public DtColumnsDefinitionMetaInfoReader(ADtColumnsDefinitionTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        return null;
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        CellKey cellKey = CellKeyFactory.getCellKey(col, row);
        Pair<CompositeMethod, String> value = expressions.get(cellKey);
        if (value != null) {
            String stringValue = value.getValue();
            if (stringValue != null) {
                List<NodeUsage> nodeUsages = null;
                nodeUsages = new ArrayList<>();
                CompositeMethod method = value.getKey();
                int startIndex = 0;
                List<NodeUsage> parsedNodeUsages = OpenLCellExpressionsCompiler
                    .getNodeUsages(method, stringValue.substring(startIndex), startIndex);
                nodeUsages.addAll(parsedNodeUsages);
                return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages, false);
            }
        }

        // Link to input parameters
        Triple<IOpenMethodHeader, String, Integer> value1 = inputs.get(cellKey);
        if (value1 != null) {
            List<NodeUsage> nodeUsages = new ArrayList<>();
            IOpenMethodHeader header = value1.getLeft();
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                IOpenClass parameterType = header.getSignature().getParameterType(i);
                IMetaInfo metaInfo = parameterType.getMetaInfo();
                while (metaInfo == null && parameterType.isArray()) {
                    parameterType = parameterType.getComponentClass();
                    metaInfo = parameterType.getMetaInfo();
                }
                if (metaInfo != null && header instanceof OpenMethodHeader) {
                    OpenMethodHeader openMethodHeader = (OpenMethodHeader) header;
                    ILocation[] paramTypeLocations = openMethodHeader.getParamTypeLocations();
                    ILocation sourceLocation = paramTypeLocations[i];
                    TextInfo text = new TextInfo(value1.getMiddle());
                    int start = sourceLocation.getStart().getAbsolutePosition(text) - value1.getRight();
                    int end = sourceLocation.getEnd().getAbsolutePosition(text) - value1.getRight();
                    nodeUsages.add(new SimpleNodeUsage(start,
                        end,
                        metaInfo.getDisplayName(INamedThing.SHORT),
                        metaInfo.getSourceUrl(),
                        NodeType.DATATYPE));
                }
            }

            if (CollectionUtils.isNotEmpty(nodeUsages)) {
                return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
            }
        }

        // Link for parameters
        Pair<IParameterDeclaration, String> value2 = parameters.get(cellKey);
        if (value2 != null) {
            IOpenClass type = value2.getKey().getType();
            while (type.getMetaInfo() == null && type.isArray()) {
                type = type.getComponentClass();
            }
            IMetaInfo metaInfo = type.getMetaInfo();
            if (metaInfo != null) {
                StringSourceCodeModule source = new StringSourceCodeModule(value2.getValue(),
                    getTableSyntaxNode().getUri());
                IdentifierNode[] paramNodes;
                try {
                    paramNodes = Tokenizer.tokenize(source, "[] \n\r");
                } catch (OpenLCompilationException e) {
                    return null;
                }
                if (paramNodes.length > 0) {
                    SimpleNodeUsage nodeUsage = new SimpleNodeUsage(paramNodes[0],
                        metaInfo.getDisplayName(INamedThing.SHORT),
                        metaInfo.getSourceUrl(),
                        NodeType.DATATYPE);
                    return new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
                }
            }

        }
        return null;

    }

    public void addExpression(int col, int row, CompositeMethod compositeMethod, String expression) {
        expressions.put(CellKeyFactory.getCellKey(col, row), Pair.of(compositeMethod, expression));
    }

    public void addInput(int col, int row, IOpenMethodHeader header, String text, int from) {
        this.inputs.put(CellKeyFactory.getCellKey(col, row), Triple.of(header, text, from));
    }

    public void addParameter(int col, int row, IParameterDeclaration parameterDeclaration, String text) {
        this.parameters.put(CellKeyFactory.getCellKey(col, row), Pair.of(parameterDeclaration, text));
    }

}
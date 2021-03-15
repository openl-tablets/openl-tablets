package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.ConditionHelper;
import org.openl.rules.fuzzy.OpenLFuzzyUtils;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DtColumnsDefinitionMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public abstract class ADtColumnsDefinitionTableBoundNode extends ATableBoundNode implements IMemberBoundNode {
    private String tableName;
    private final OpenL openl;
    private final IBindingContext bindingContext;
    private XlsModuleOpenClass xlsModuleOpenClass;
    private final Map<DTColumnsDefinition, PreBindDetails> definitions = new HashMap<>();

    public ADtColumnsDefinitionTableBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IBindingContext bindingContext) {
        super(tableSyntaxNode);
        this.openl = Objects.requireNonNull(openl, "openl cannot be null");
        this.bindingContext = Objects.requireNonNull(bindingContext, "bindingContext cannot be null");
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        this.xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
        TableSyntaxNode tsn = getTableSyntaxNode();
        ILogicalTable tableBody = tsn.getTableBody();
        if (tableBody == null) {
            return;
        }
        int[] tableStructure = getTableStructure(tableBody);
        int w = tableStructure.length;
        if (w != 4) {
            tableBody = tableBody.transpose();
            tableStructure = getTableStructure(tableBody);
            w = tableStructure.length;
            if (w != 4) {
                BindHelper.processError(
                    "Wrong table structure: Expected 4 columns table: <Inputs> <Expression> <Parameter> <Title>.",
                    getTableSyntaxNode(),
                    bindingContext);
                return;
            }
        }

        int i = 0;
        int[] headerIndexes = getHeaderIndexes(tableBody, tableStructure);
        if (headerIndexes != DEFAULT_HEADER_INDEXES) {
            i = tableBody.getSource().getCell(0, 0).getHeight();
        } else {
            ILogicalTable tableBodyT = tableBody.transpose();
            int[] tableStructureT = getTableStructure(tableBodyT);
            if (tableStructureT.length == 4) {
                int[] headerIndexesT = getHeaderIndexes(tableBodyT, tableStructureT);
                i = tableBodyT.getSource().getCell(0, 0).getHeight();
                tableBody = tableBodyT;
                tableStructure = tableStructureT;
                headerIndexes = headerIndexesT;
            }
        }

        int h = tableBody.getSource().getHeight();

        final ILogicalTable tableBody1 = tableBody;
        final int[] tableStructure1 = tableStructure;
        final int[] headerIndexes1 = headerIndexes;

        while (i < h) {
            String signatureCode1 = tableBody.getSource()
                .getCell(tableStructure[headerIndexes[INPUTS_INDEX]], i)
                .getStringValue();
            ICell inputsCell = tableBody.getSource().getCell(tableStructure[headerIndexes[INPUTS_INDEX]], i);
            if (StringUtils.isEmpty(signatureCode1)) {
                signatureCode1 = StringUtils.EMPTY;
            }
            final String signatureCode = signatureCode1;
            IGridTable expressionTable = tableBody.getSource()
                .getSubtable(tableStructure[headerIndexes[EXPRESSION_INDEX]], i, 1, 1);
            ICell expressionCell = tableBody.getSource().getCell(tableStructure[headerIndexes[EXPRESSION_INDEX]], i);
            boolean finished = false;
            String prefix = JavaOpenClass.VOID.getName() + " " + RandomStringUtils.random(16, true, false) + "(";
            String headerCode = prefix + signatureCode + ")";
            IOpenMethodHeader header;
            try {
                header = OpenLManager.makeMethodHeader(getOpenl(),
                    new org.openl.source.impl.StringSourceCodeModule(headerCode, null),
                    bindingContext);
            } catch (OpenLCompilationException e) {
                throw new IllegalStateException("Illegal state", e);
            }
            if (!bindingContext.isExecutionMode()) {
                addMetaInfoForInputs(header, inputsCell, headerCode, prefix.length());
            }
            int j = 0;
            int j1 = 0;
            Map<String, List<IParameterDeclaration>> parameters = new HashMap<>();
            List<IParameterDeclaration> parametersForMergedTitle = new ArrayList<>();
            Set<String> uniqueSetOfParameters = new HashSet<>();
            Set<String> uniqueSetOfTitles = new HashSet<>();
            String title = null;
            Boolean singleParameter = null;
            GridCellSourceCodeModule pGridCellSourceCodeModule = null;
            int d = expressionTable.getCell(0, 0).getHeight();
            while (j < d) {
                if (pGridCellSourceCodeModule != null && parametersForMergedTitle
                    .size() == 1 && parametersForMergedTitle.get(0) == null) {
                    String errMsg = "Parameter cell format: <type> or <type> <name>";
                    BindHelper.processError(errMsg, pGridCellSourceCodeModule, bindingContext);
                    finished = true;
                    break;
                }
                IGridTable pCodeTable = tableBody1.getSource()
                    .getSubtable(tableStructure1[headerIndexes1[PARAMETER_INDEX]], i + j, 1, 1);
                if (singleParameter == null) {
                    singleParameter = j + pCodeTable.getCell(0, 0).getHeight() >= d;
                }
                pGridCellSourceCodeModule = new GridCellSourceCodeModule(pCodeTable, bindingContext);

                ParameterDeclaration parameterDeclaration = null;
                String code = ((IOpenSourceCodeModule) pGridCellSourceCodeModule).getCode();
                if (StringUtils.isNotBlank(code)) {
                    String[] parts = code.split("\\s+");

                    if (parts.length > 2) {
                        String errMsg = "Parameter cell format: <type> or <type> <name>";
                        BindHelper.processError(errMsg, pGridCellSourceCodeModule, bindingContext);
                        finished = true;
                        break;
                    } else {
                        IOpenClass type = RuleRowHelper.getType(parts[0], pGridCellSourceCodeModule, bindingContext);

                        if (parts.length == 1) {
                            parameterDeclaration = new ParameterDeclaration(type, null);
                        } else {
                            parameterDeclaration = new ParameterDeclaration(type, parts[1]);
                        }
                    }
                }

                if (!parametersForMergedTitle.isEmpty() && parameterDeclaration == null) {
                    String errMsg = "Parameter cell format: <type> or <type> <name>";
                    BindHelper.processError(errMsg, pGridCellSourceCodeModule, bindingContext);
                    finished = true;
                    break;
                }

                parametersForMergedTitle.add(parameterDeclaration);
                if (parameterDeclaration != null) {
                    if (parameterDeclaration.getName() != null) {
                        if (uniqueSetOfParameters.contains(parameterDeclaration.getName())) {
                            String errorMessage = "Parameter '" + parameterDeclaration
                                .getName() + "' is already defined.";
                            BindHelper.processError(errorMessage, pGridCellSourceCodeModule, bindingContext);
                            finished = true;
                            break;
                        }
                        uniqueSetOfParameters.add(parameterDeclaration.getName());
                    }
                    if (!bindingContext.isExecutionMode()) {
                        ICell parameterCell = tableBody1.getSource()
                            .getCell(tableStructure1[headerIndexes1[PARAMETER_INDEX]], i + j);
                        addMetaInfoForParameter(parameterDeclaration, parameterCell);
                    }
                }

                if (j1 <= j) {
                    IGridTable tCodeTable = tableBody1.getSource()
                        .getSubtable(tableStructure1[headerIndexes1[TITLE_INDEX]], i + j, 1, 1);
                    String title1 = tCodeTable.getCell(0, 0).getStringValue();
                    if (StringUtils.isEmpty(title1)) {
                        GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(tCodeTable,
                            bindingContext);
                        BindHelper.processError("Title cannot be empty.", tGridCellSourceCodeModule, bindingContext);
                        finished = true;
                        break;
                    }
                    title = OpenLFuzzyUtils.toTokenString(title1);
                    if (uniqueSetOfTitles.contains(title)) {
                        GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(tCodeTable,
                            bindingContext);
                        BindHelper.processError("Title '" + title1 + "' is already defined.",
                            tGridCellSourceCodeModule,
                            bindingContext);
                        finished = true;
                        break;
                    }
                    uniqueSetOfTitles.add(title);
                    j1 = j1 + tCodeTable.getCell(0, 0).getHeight();
                }

                j = j + pCodeTable.getCell(0, 0).getHeight();
                if (j1 <= j || j >= d) {
                    parameters.put(title, parametersForMergedTitle);
                    parametersForMergedTitle = new ArrayList<>();
                }
            }
            if (!finished) {
                createAndAddDefinition(header, parameters, expressionTable, expressionCell);
            }
            i = i + expressionTable.getCell(0, 0).getHeight();
        }
    }

    private IBindingContext buildDtHeaderBindingContext() {
        DecisionTableDataType ruleExecutionType = new DecisionTableDataType(null, "DecisionTableDataType", openl);
        IBindingContext dtHeaderBindingContext = new ComponentBindingContext(bindingContext, ruleExecutionType);
        return dtHeaderBindingContext;
    }

    private static class PreBindDetails {
        private final IGridTable expressionTable;
        private final ICell expressionCell;
        private final IOpenMethodHeader header;

        public PreBindDetails(IGridTable expressionTable, ICell expressionCell, IOpenMethodHeader header) {
            this.expressionTable = expressionTable;
            this.expressionCell = expressionCell;
            this.header = header;
        }
    }

    public OpenL getOpenl() {
        return openl;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return null;
    }

    protected XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) {
        definitions.clear();
    }

    protected abstract DTColumnsDefinition createDefinition(IOpenMethodHeader header,
            String expression,
            Map<String, List<IParameterDeclaration>> dtDTColumnsDefinitionParameters);

    protected final void createAndAddDefinition(IOpenMethodHeader header,
            Map<String, List<IParameterDeclaration>> parameters,
            IGridTable expressionTable,
            ICell expressionCell) {
        DTColumnsDefinition dtColumnsDefinition = createDefinition(header,
            expressionCell.getStringValue() != null ? expressionCell.getStringValue() : StringUtils.EMPTY,
            parameters);
        definitions.put(dtColumnsDefinition, new PreBindDetails(expressionTable, expressionCell, header));
        getXlsModuleOpenClass().getXlsDefinitions().addDtColumnsDefinition(dtColumnsDefinition);
    }

    private int[] getHeaderIndexes(ILogicalTable tableBody, int[] tableStructure) {
        int[] headerIndexes = new int[4];
        int j = 0;
        int k = 0;
        while (j < tableStructure.length) {
            String d = tableBody.getSource().getCell(tableStructure[j], 0).getStringValue();
            if ("Title".equalsIgnoreCase(d)) {
                headerIndexes[TITLE_INDEX] = j;
                k++;
            } else if ("Parameter".equalsIgnoreCase(d)) {
                headerIndexes[PARAMETER_INDEX] = j;
                k++;
            } else if ("Expression".equalsIgnoreCase(d)) {
                headerIndexes[EXPRESSION_INDEX] = j;
                k++;
            } else if ("Inputs".equalsIgnoreCase(d)) {
                headerIndexes[INPUTS_INDEX] = j;
                k++;
            }
            j++;

        }
        if (k == 4) {
            return headerIndexes;
        }
        return DEFAULT_HEADER_INDEXES;
    }

    private static final int[] DEFAULT_HEADER_INDEXES = new int[] { 0, 1, 2, 3 };
    private static final int INPUTS_INDEX = 0;
    private static final int EXPRESSION_INDEX = 1;
    private static final int PARAMETER_INDEX = 2;
    private static final int TITLE_INDEX = 3;

    private static int[] getTableStructure(ILogicalTable originalTable) {
        int w = originalTable.getSource().getWidth();
        int h = originalTable.getSource().getHeight();
        int i = 0;
        List<Integer> t = new ArrayList<>();
        while (i < w) {
            t.add(i);
            i = i + originalTable.getSource().getCell(i, h - 1).getWidth();
        }
        return ArrayUtils.toPrimitive(t.toArray(new Integer[] {}));
    }

    private boolean isParameterIsUsed(CompositeMethod compositeMethod,
            Collection<IParameterDeclaration> parameters) {
        List<IdentifierNode> identifierNodes = DecisionTableUtils.retrieveIdentifierNodes(compositeMethod);
        for (IdentifierNode identifierNode : identifierNodes) {
            for (IParameterDeclaration parameterDeclaration : parameters) {
                if (parameterDeclaration != null && Objects.equals(identifierNode.getIdentifier(),
                    parameterDeclaration.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void finalizeBind(IBindingContext cxt) {
        IBindingContext dtHeaderBindingContext = buildDtHeaderBindingContext();
        for (Map.Entry<DTColumnsDefinition, PreBindDetails> entry : definitions.entrySet()) {
            compileValidateExpressionAndAddDefinition(entry.getKey(), entry.getValue(), dtHeaderBindingContext);
        }
    }

    private void compileValidateExpressionAndAddDefinition(DTColumnsDefinition dtColumnsDefinition,
            PreBindDetails preBindDetail,
            IBindingContext dtHeaderBindingContext) {
        IParameterDeclaration[] allParameterDeclarations = dtColumnsDefinition.getParameters()
            .stream()
            .filter(e -> e != null && e.getName() != null)
            .collect(Collectors.toList())
            .toArray(IParameterDeclaration.EMPTY);
        IMethodSignature newSignature = ((MethodSignature) preBindDetail.header.getSignature())
            .merge(allParameterDeclarations);

        GridCellSourceCodeModule expressionCellSourceCodeModule = new GridCellSourceCodeModule(
            preBindDetail.expressionTable,
            bindingContext);

        CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(getOpenl(),
            expressionCellSourceCodeModule,
            preBindDetail.header.getName(),
            newSignature,
            getXlsModuleOpenClass(),
            dtHeaderBindingContext);

        dtColumnsDefinition.setCompositeMethod(compositeMethod);

        validate(preBindDetail.header,
            dtColumnsDefinition.getParameters(),
            expressionCellSourceCodeModule,
            compositeMethod);

        if (!bindingContext.isExecutionMode()) {
            addMetaInfoForExpression(compositeMethod, preBindDetail.expressionCell);
        }
    }

    private void validate(IOpenMethodHeader header,
            Collection<IParameterDeclaration> parameters,
            GridCellSourceCodeModule expressionCellSourceCodeModule,
            CompositeMethod compositeMethod) {
        if (StringUtils.isBlank(expressionCellSourceCodeModule.getCode())) {
            if (isConditions()) {
                BindHelper.processError("Expression is required for a condition.",
                    expressionCellSourceCodeModule,
                    bindingContext);
                return;
            } else if (isActions()) {
                BindHelper.processError("Expression is required for an action.",
                    expressionCellSourceCodeModule,
                    bindingContext);
                return;
            }
        }
        if (isConditions() && compositeMethod.getType().getInstanceClass() != boolean.class && compositeMethod.getType()
            .getInstanceClass() != Boolean.class) {

            if (isSimplifiedSyntaxIsUsed(expressionCellSourceCodeModule.getCode(), header.getSignature())) {
                validateConditionType(compositeMethod, expressionCellSourceCodeModule, parameters, bindingContext);
            } else {
                if (isParameterIsUsed(compositeMethod, parameters)) {
                    BindHelper.processError("Condition expression must return a boolean type.",
                        expressionCellSourceCodeModule,
                        bindingContext);
                } else {
                    validateConditionType(compositeMethod, expressionCellSourceCodeModule, parameters, bindingContext);
                }
            }
        }
    }

    private void validateConditionType(CompositeMethod compositeMethod,
            GridCellSourceCodeModule expressionCellSourceCodeModule,
            Collection<IParameterDeclaration> parameters,
            IBindingContext cxt) {
        IOpenClass parameterType = null;
        int parameterCount = 0;
        for (IParameterDeclaration paramType : parameters) {
            parameterCount++;
            if (paramType != null) {
                if (parameterType == null) {
                    parameterType = paramType.getType();
                } else if (!Objects.equals(parameterType, paramType.getType())) {
                    BindHelper.processError("Condition expression must return a boolean type.",
                        expressionCellSourceCodeModule,
                        cxt);
                    return;
                }
            }
        }

        if (parameterCount > 2) {
            BindHelper.processError("Condition expression type is incompatible with condition parameter type.",
                expressionCellSourceCodeModule,
                cxt);
            return;
        }

        if (parameterType != null) {
            boolean f1 = ConditionHelper.findConditionCasts(parameterType, compositeMethod.getType(), cxt)
                .atLeastOneExists();
            boolean f2 = parameterType.isArray() && ConditionHelper
                .findConditionCasts(parameterType.getComponentClass(), compositeMethod.getType(), cxt)
                .atLeastOneExists();
            if (!(f1 || f2)) {
                BindHelper.processError("Condition expression type is incompatible with condition parameter type.",
                    expressionCellSourceCodeModule,
                    cxt);
            }
        }
    }

    private boolean isSimplifiedSyntaxIsUsed(String code, IMethodSignature signature) {
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            if (Objects.equals(code, signature.getParameterName(i))) {
                return true;
            }
        }
        return false;
    }

    private void addMetaInfoForExpression(CompositeMethod compositeMethod, ICell cell) {
        MetaInfoReader metaInfoReader = getTableSyntaxNode().getMetaInfoReader();
        if (metaInfoReader instanceof DtColumnsDefinitionMetaInfoReader) {
            DtColumnsDefinitionMetaInfoReader dtColumnsDefinitionMetaInfoReader = (DtColumnsDefinitionMetaInfoReader) metaInfoReader;
            dtColumnsDefinitionMetaInfoReader
                .addExpression(cell.getAbsoluteColumn(), cell.getAbsoluteRow(), compositeMethod, cell.getStringValue());
        }
    }

    private void addMetaInfoForInputs(IOpenMethodHeader header, ICell cell, String text, int from) {
        MetaInfoReader metaInfoReader = getTableSyntaxNode().getMetaInfoReader();
        if (metaInfoReader instanceof DtColumnsDefinitionMetaInfoReader) {
            DtColumnsDefinitionMetaInfoReader dtColumnsDefinitionMetaInfoReader = (DtColumnsDefinitionMetaInfoReader) metaInfoReader;
            dtColumnsDefinitionMetaInfoReader
                .addInput(cell.getAbsoluteColumn(), cell.getAbsoluteRow(), header, text, from);
        }
    }

    private void addMetaInfoForParameter(IParameterDeclaration parameterDeclaration, ICell cell) {
        MetaInfoReader metaInfoReader = getTableSyntaxNode().getMetaInfoReader();
        if (metaInfoReader instanceof DtColumnsDefinitionMetaInfoReader) {
            DtColumnsDefinitionMetaInfoReader dtColumnsDefinitionMetaInfoReader = (DtColumnsDefinitionMetaInfoReader) metaInfoReader;
            dtColumnsDefinitionMetaInfoReader.addParameter(cell.getAbsoluteColumn(),
                cell.getAbsoluteRow(),
                parameterDeclaration,
                cell.getStringValue());
        }
    }

    protected abstract boolean isConditions();

    protected abstract boolean isActions();

    protected abstract boolean isReturns();
}

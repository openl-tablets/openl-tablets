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
    private XlsModuleOpenClass xlsModuleOpenClass;

    public ADtColumnsDefinitionTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl) {
        super(tableSyntaxNode);
        this.openl = openl;
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
    }

    protected abstract DTColumnsDefinition createDefinition(
            Map<String, List<IParameterDeclaration>> parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod,
            IBindingContext bindingContext);

    protected final void createAndAddDefinition(Map<String, List<IParameterDeclaration>> parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod,
            IBindingContext bindingContext) {
        DTColumnsDefinition definition = createDefinition(parameterDeclarations,
            header,
            compositeMethod,
            bindingContext);
        getXlsModuleOpenClass().getXlsDefinitions().addDtColumnsDefinition(definition);
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

    private boolean isLocalParameterIsUsed(CompositeMethod compositeMethod,
            Collection<List<IParameterDeclaration>> localParameters) {
        List<IdentifierNode> identifierNodes = DecisionTableUtils.retrieveIdentifierNodes(compositeMethod);
        for (IdentifierNode identifierNode : identifierNodes) {
            for (List<IParameterDeclaration> parameterDeclarations : localParameters) {
                for (IParameterDeclaration parameterDeclaration : parameterDeclarations) {
                    if (parameterDeclaration != null && Objects.equals(identifierNode.getIdentifier(),
                        parameterDeclaration.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void finalizeBind(IBindingContext cxt) {
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
                    cxt);
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

        DecisionTableDataType ruleExecutionType = new DecisionTableDataType(null,
            "DecisionTableDataType",
            openl,
            false);
        IBindingContext dtHeaderBindingContext = new ComponentBindingContext(cxt, ruleExecutionType);

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
            int d = expressionTable.getCell(0, 0).getHeight();
            final int z = i;
            process(cxt,
                tableBody1,
                tableStructure1,
                headerIndexes1,
                dtHeaderBindingContext,
                inputsCell,
                signatureCode,
                expressionTable,
                expressionCell,
                d,
                z);
            i = i + d;
        }
    }

    private void process(IBindingContext cxt,
            ILogicalTable tableBody1,
            int[] tableStructure1,
            int[] headerIndexes1,
            IBindingContext dtHeaderBindingContext,
            ICell inputsCell,
            String signatureCode,
            IGridTable expressionTable,
            ICell expressionCell,
            int d,
            int z) {
        String prefix = JavaOpenClass.VOID.getName() + " " + RandomStringUtils.random(16, true, false) + "(";
        String headerCode = prefix + signatureCode + ")";
        IOpenMethodHeader header;
        try {
            header = OpenLManager.makeMethodHeader(getOpenl(),
                new org.openl.source.impl.StringSourceCodeModule(headerCode, null),
                dtHeaderBindingContext);
        } catch (OpenLCompilationException e) {
            throw new IllegalStateException("Illegal state", e);
        }
        if (!cxt.isExecutionMode()) {
            addMetaInfoForInputs(header, inputsCell, headerCode, prefix.length());
        }
        int j = 0;
        int j1 = 0;
        Map<String, List<IParameterDeclaration>> localParameters = new HashMap<>();
        List<IParameterDeclaration> parametersForMergedTitle = new ArrayList<>();
        Set<String> uniqueSetOfParameters = new HashSet<>();
        Set<String> uniqueSetOfTitles = new HashSet<>();
        String title = null;
        Boolean singleParameter = null;
        GridCellSourceCodeModule pGridCellSourceCodeModule = null;
        while (j < d) {
            if (pGridCellSourceCodeModule != null && parametersForMergedTitle.size() == 1 && parametersForMergedTitle
                .get(0) == null) {
                String errMsg = "Parameter cell format: <type> or <type> <name>";
                BindHelper.processError(errMsg, pGridCellSourceCodeModule, cxt);
                return;
            }
            IGridTable pCodeTable = tableBody1.getSource()
                .getSubtable(tableStructure1[headerIndexes1[PARAMETER_INDEX]], z + j, 1, 1);
            if (singleParameter == null) {
                singleParameter = j + pCodeTable.getCell(0, 0).getHeight() >= d;
            }
            pGridCellSourceCodeModule = new GridCellSourceCodeModule(pCodeTable, cxt);

            ParameterDeclaration parameterDeclaration = null;
            String code = ((IOpenSourceCodeModule) pGridCellSourceCodeModule).getCode();
            if (StringUtils.isNotBlank(code)) {
                String[] parts = code.split("\\s+");

                if (parts.length > 2) {
                    String errMsg = "Parameter cell format: <type> or <type> <name>";
                    BindHelper.processError(errMsg, pGridCellSourceCodeModule, cxt);
                    return;
                } else {
                    IOpenClass type = RuleRowHelper.getType(parts[0], pGridCellSourceCodeModule, cxt);

                    if (parts.length == 1) {
                        parameterDeclaration = new ParameterDeclaration(type, null);
                    } else {
                        parameterDeclaration = new ParameterDeclaration(type, parts[1]);
                    }
                }
            }

            if (!parametersForMergedTitle.isEmpty() && parameterDeclaration == null) {
                String errMsg = "Parameter cell format: <type> or <type> <name>";
                BindHelper.processError(errMsg, pGridCellSourceCodeModule, cxt);
                return;
            }

            parametersForMergedTitle.add(parameterDeclaration);
            if (parameterDeclaration != null) {
                if (parameterDeclaration.getName() != null) {
                    if (uniqueSetOfParameters.contains(parameterDeclaration.getName())) {
                        String errorMessage = "Parameter '" + parameterDeclaration.getName() + "' is already defined.";
                        BindHelper.processError(errorMessage, pGridCellSourceCodeModule, cxt);
                        return;
                    }
                    uniqueSetOfParameters.add(parameterDeclaration.getName());
                }
                if (!cxt.isExecutionMode()) {
                    ICell parameterCell = tableBody1.getSource()
                        .getCell(tableStructure1[headerIndexes1[PARAMETER_INDEX]], z + j);
                    addMetaInfoForParameter(parameterDeclaration, parameterCell);
                }
            }

            if (j1 <= j) {
                IGridTable tCodeTable = tableBody1.getSource()
                    .getSubtable(tableStructure1[headerIndexes1[TITLE_INDEX]], z + j, 1, 1);
                String title1 = tCodeTable.getCell(0, 0).getStringValue();
                if (StringUtils.isEmpty(title1)) {
                    GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(tCodeTable, cxt);
                    BindHelper.processError("Title cannot be empty.", tGridCellSourceCodeModule, cxt);
                    return;
                }
                title = OpenLFuzzyUtils.toTokenString(title1);
                if (uniqueSetOfTitles.contains(title)) {
                    GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(tCodeTable, cxt);
                    BindHelper
                        .processError("Title '" + title1 + "' is already defined.", tGridCellSourceCodeModule, cxt);
                    return;
                }
                uniqueSetOfTitles.add(title);
                j1 = j1 + tCodeTable.getCell(0, 0).getHeight();
            }

            j = j + pCodeTable.getCell(0, 0).getHeight();
            if (j1 <= j || j >= d) {
                localParameters.put(title, parametersForMergedTitle);
                parametersForMergedTitle = new ArrayList<>();
            }
        }

        IParameterDeclaration[] allParameterDeclarations = localParameters.values()
            .stream()
            .flatMap(List::stream)
            .filter(e -> e != null && e.getName() != null)
            .collect(Collectors.toList())
            .toArray(IParameterDeclaration.EMPTY);

        IMethodSignature newSignature = ((MethodSignature) header.getSignature()).merge(allParameterDeclarations);

        GridCellSourceCodeModule expressionCellSourceCodeModule = new GridCellSourceCodeModule(expressionTable, cxt);

        CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(getOpenl(),
            expressionCellSourceCodeModule,
            header.getName(),
            newSignature,
            getXlsModuleOpenClass(),
            dtHeaderBindingContext);

        validate(header, localParameters, expressionCellSourceCodeModule, compositeMethod, cxt);

        if (!cxt.isExecutionMode()) {
            addMetaInfoForExpression(compositeMethod, expressionCell);
        }

        createAndAddDefinition(localParameters, header, compositeMethod, cxt);
    }

    private void validate(IOpenMethodHeader header,
            Map<String, List<IParameterDeclaration>> localParameters,
            GridCellSourceCodeModule expressionCellSourceCodeModule,
            CompositeMethod compositeMethod,
            IBindingContext cxt) {
        if (StringUtils.isBlank(expressionCellSourceCodeModule.getCode())) {
            if (isConditions()) {
                BindHelper.processError("Expression is required for a condition.", expressionCellSourceCodeModule, cxt);
                return;
            } else if (isActions()) {
                BindHelper.processError("Expression is required for an action.", expressionCellSourceCodeModule, cxt);
                return;
            }
        }
        if (isConditions() && compositeMethod.getType().getInstanceClass() != boolean.class && compositeMethod.getType()
            .getInstanceClass() != Boolean.class) {

            if (isSimplifiedSyntaxIsUsed(expressionCellSourceCodeModule.getCode(), header.getSignature())) {
                validateConditionType(compositeMethod, expressionCellSourceCodeModule, localParameters, cxt);
            } else {
                if (isLocalParameterIsUsed(compositeMethod, localParameters.values())) {
                    BindHelper.processError("Condition expression must return a boolean type.",
                        expressionCellSourceCodeModule,
                        cxt);
                } else {
                    validateConditionType(compositeMethod, expressionCellSourceCodeModule, localParameters, cxt);
                }
            }
        }
    }

    private void validateConditionType(CompositeMethod compositeMethod,
            GridCellSourceCodeModule expressionCellSourceCodeModule,
            Map<String, List<IParameterDeclaration>> localParameters,
            IBindingContext cxt) {
        IOpenClass parameterType = null;
        int localParameterCount = 0;
        for (List<IParameterDeclaration> paramTypes : localParameters.values()) {
            for (IParameterDeclaration paramType : paramTypes) {
                localParameterCount++;
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
        }

        if (localParameterCount > 2) {
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

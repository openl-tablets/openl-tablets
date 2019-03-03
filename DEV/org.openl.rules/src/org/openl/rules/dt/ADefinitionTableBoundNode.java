package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.Runnable;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public abstract class ADefinitionTableBoundNode extends ATableBoundNode implements IMemberBoundNode {
    private String tableName;
    private OpenL openl;
    private XlsModuleOpenClass xlsModuleOpenClass;
    private boolean mandatoryParameterName;

    public ADefinitionTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl, boolean mandatoryParameterName) {
        super(tableSyntaxNode);
        this.openl = openl;
        this.mandatoryParameterName = mandatoryParameterName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

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
    
    protected IParameterDeclaration getParameterDeclaration(IOpenSourceCodeModule paramSource,
            IBindingContext bindingContext) throws OpenLCompilationException {

        IdentifierNode[] nodes = Tokenizer.tokenize(paramSource, " \n\r");

        if (nodes.length > 2 || mandatoryParameterName && nodes.length != 2) {
            String errMsg = "Parameter format: <type> <name>";
            throw SyntaxNodeExceptionUtils.createError(errMsg, null, null, paramSource);
        }

        if (nodes.length == 0) {
            return null;
        }

        String typeCode = nodes[0].getIdentifier();
        IOpenClass type = RuleRowHelper.getType(typeCode, bindingContext);

        if (type == null) {
            throw SyntaxNodeExceptionUtils.createError("Type '" + typeCode + "'is not found", nodes[0]);
        }

        if (nodes.length == 1) {
            return new ParameterDeclaration(type, null);
        }

        String name = nodes[1].getIdentifier();

        return new ParameterDeclaration(type, name);
    }

    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    public void removeDebugInformation(IBindingContext cxt) {
    }
    
    protected abstract void createAndAddDefinition(String[] titles, IParameterDeclaration[] parameterDeclarations, IOpenMethodHeader header, CompositeMethod compositeMethod);

    
    private int[] getHeaderIndexes(ILogicalTable tableBody, int[] tableStructure) {
        Set<String> headerTokens = new HashSet<String>();
        int[] headerIndexes = new int[4];
        int j = 0;
        int k = 0;
        while (j < tableStructure.length) {
            String d = tableBody.getSource().getCell(tableStructure[j], 0).getStringValue();
            headerTokens.add(d);
            if ("Title".equalsIgnoreCase(d)) {
                headerIndexes[0] = j;
                k++;
            } else if ("Parameter".equalsIgnoreCase(d)) {
                headerIndexes[1] = j;
                k++;
            } else if ("Expression".equalsIgnoreCase(d)) {
                headerIndexes[2] = j;
                k++;
            } else if ("Inputs".equalsIgnoreCase(d)) {
                headerIndexes[3] = j;
                k++;
            }
            j++;;
        }
        if (k == 4) {
            return headerIndexes;
        }
        return DEFAULT_HEADER_INDEXES;
    }
    
    private static final int[] DEFAULT_HEADER_INDEXES = new int[] { 0, 1, 2, 3 };
    private static final int INPUTS_INDEX = 3;
    private static final int EXPRESSION_INDEX = 2;
    private static final int PARAMETER_INDEX = 1;
    private static final int TITLE_INDEX = 0;
    
    private static int[] getTableStructure(ILogicalTable originalTable) {
        int w = originalTable.getSource().getWidth();
        int i = 0;
        List<Integer> t = new ArrayList<>();
        while (i < w) {
            t.add(i);
            i = i + originalTable.getSource().getCell(i, 0).getWidth();
        }
        return ArrayUtils.toPrimitive(t.toArray(new Integer[] {}));
    }
    
    public void finalizeBind(IBindingContext cxt) throws Exception {
        TableSyntaxNode tsn = getTableSyntaxNode();
        ILogicalTable tableBody = tsn.getTableBody();

        int[] tableStructure = getTableStructure(tableBody);
        int w = tableStructure.length;
        if (w != 4) {
            throw SyntaxNodeExceptionUtils.createError(
                "Wrong table structure: Expected 4 columns table: <Title> <Parameter> <Expression> <Inputs>.",
                getTableSyntaxNode());
        }
        int h = tableBody.getSource().getHeight();
        int i = 0;

        final int[] headerIndexes = getHeaderIndexes(tableBody, tableStructure);
        if (headerIndexes != DEFAULT_HEADER_INDEXES) {
            i = tableBody.getSource().getCell(0, 0).getHeight();
        } 
        
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        while (i < h) {
            String signatureCode1 = tableBody.getSource().getCell(tableStructure[headerIndexes[INPUTS_INDEX]], i).getStringValue();
            if (StringUtils.isEmpty(signatureCode1)) {
                signatureCode1 = StringUtils.EMPTY;
            }
            final String signatureCode = signatureCode1;
            IGridTable expressionTable = tableBody.getSource().getSubtable(tableStructure[headerIndexes[EXPRESSION_INDEX]], i, 1, 1);
            int d = expressionTable.getCell(0, 0).getHeight();
            final int z = i;
            syntaxNodeExceptionCollector.run(new Runnable() {
                
                @Override
                public void run() throws Exception {
                    IOpenMethodHeader header;
                    try {
                        header = OpenLManager.makeMethodHeader(getOpenl(),
                            new org.openl.source.impl.StringSourceCodeModule(
                                JavaOpenClass.VOID.getName() + " " + RandomStringUtils
                                    .random(16, true, false) + "(" + signatureCode + ")",
                                null),
                            cxt);
                    } catch (CompositeSyntaxNodeException e) {
                        GridCellSourceCodeModule pGridCellSourceCodeModule = new GridCellSourceCodeModule(
                            expressionTable,
                            cxt);
                        throw SyntaxNodeExceptionUtils.createError(String.format("Failed to parse the cell '%s'", pGridCellSourceCodeModule.getCode()), pGridCellSourceCodeModule);
                    } 
                    int j = 0;

                    List<IParameterDeclaration> localParameterDeclarations = new ArrayList<>();
                    List<IParameterDeclaration> parameterDeclarations = new ArrayList<>();
                    Set<String> uniqueSet = new HashSet<>();
                    while (j < d) {
                        IGridTable pCodeTable = tableBody.getSource().getSubtable(tableStructure[headerIndexes[PARAMETER_INDEX]], z + j, 1, 1);
                        GridCellSourceCodeModule pGridCellSourceCodeModule = new GridCellSourceCodeModule(
                            pCodeTable,
                            cxt);
                        IParameterDeclaration parameterDeclaration = getParameterDeclaration(pGridCellSourceCodeModule,
                            cxt);
                        if (parameterDeclaration != null) {
                            localParameterDeclarations.add(parameterDeclaration);
                            if (parameterDeclaration.getName() != null) {
                                if (uniqueSet.contains(parameterDeclaration.getName())) {
                                    throw SyntaxNodeExceptionUtils.createError("Parameter '" + parameterDeclaration.getName() + "' has already been defined.",
                                        pGridCellSourceCodeModule);
                                }
                                uniqueSet.add(parameterDeclaration.getName());
                            }
                        }
                        parameterDeclarations.add(parameterDeclaration);
                        j = j + pCodeTable.getCell(0, 0).getHeight();
                    }
                    j = 0;
                    int k = 0;
                    uniqueSet = new HashSet<>();
                    String[] titles = new String[parameterDeclarations.size()];
                    while (j < d) {
                        IGridTable tCodeTable = tableBody.getSource().getSubtable(tableStructure[headerIndexes[TITLE_INDEX]], z + j, 1, 1);
                        String title = tCodeTable.getCell(0, 0).getStringValue();
                        if (StringUtils.isEmpty(title)) { 
                            GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(
                                tCodeTable,
                                cxt); 
                            throw SyntaxNodeExceptionUtils.createError("Title can't be empty.", tGridCellSourceCodeModule);
                        }
                        if (uniqueSet.contains(title)) {
                            GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(
                                tCodeTable,
                                cxt); 
                            throw SyntaxNodeExceptionUtils.createError("Title '" + title + "' has already been defined.",
                                tGridCellSourceCodeModule);
                        }
                        titles[k++] = title;
                        uniqueSet.add(title);
                        j = j + tCodeTable.getCell(0, 0).getHeight();
                    }

                    IMethodSignature newSignature = ((MethodSignature) header.getSignature())
                        .merge(localParameterDeclarations.toArray(new IParameterDeclaration[] {}));

                    GridCellSourceCodeModule expressionCellSourceCodeModule = new GridCellSourceCodeModule(
                        expressionTable,
                        cxt);

                    CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(getOpenl(),
                        expressionCellSourceCodeModule,
                        header.getName(),
                        newSignature,
                        getXlsModuleOpenClass(),
                        cxt);
                    
                    createAndAddDefinition(titles, parameterDeclarations.toArray(new IParameterDeclaration[] {}), header, compositeMethod);
                }
            });

            i = i + d;
        }
        syntaxNodeExceptionCollector.throwIfAny();
    }

}

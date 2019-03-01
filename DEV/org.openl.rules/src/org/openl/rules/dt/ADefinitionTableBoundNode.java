package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

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
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
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
import org.openl.syntax.exception.Runnable;

public abstract class ADefinitionTableBoundNode extends ATableBoundNode implements IMemberBoundNode {
    private String tableName;
    private OpenL openl;
    private XlsModuleOpenClass xlsModuleOpenClass;
    private int counter = 0;
    private boolean mandatoryParameterDeclarationColumn;
    private boolean mandatoryParameterName;

    public ADefinitionTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl, boolean mandatoryParameterDeclarationColumn, boolean mandatoryParameterName) {
        super(tableSyntaxNode);
        this.openl = openl;
        this.mandatoryParameterDeclarationColumn = mandatoryParameterDeclarationColumn;
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
            String errMsg = "Parameter cell format: <type> <name>";
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
            return new ParameterDeclaration(type, RandomStringUtils.random(6, true, false) + counter++);
        }

        String name = nodes[1].getIdentifier();

        return new ParameterDeclaration(type, name);
    }

    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    public void removeDebugInformation(IBindingContext cxt) {
    }
    
    protected abstract void createAndAddDefinition(String[] parameterDescriptions, IParameterDeclaration[] parameterDeclarations, IOpenMethodHeader header, CompositeMethod compositeMethod);
    
    public void finalizeBind(IBindingContext cxt) throws Exception {
        TableSyntaxNode tsn = getTableSyntaxNode();
        ILogicalTable tableBody = tsn.getTableBody();

        int w = tableBody.getWidth();
        if (w > 4 || w < 3 || mandatoryParameterDeclarationColumn && w == 3) {
            throw SyntaxNodeExceptionUtils.createError(
                "Wrong table structure: Expected 4 columns table: <Description> <Parameter Declaration> <Expression> <Header Parameters>.",
                getTableSyntaxNode());
        }
        int h = tableBody.getHeight();
        int i = 0;
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        while (i < h) {
            String signatureCode1 = tableBody.getCell(w - 1, i).getStringValue();
            if (StringUtils.isEmpty(signatureCode1)) {
                signatureCode1 = StringUtils.EMPTY;
            }
            final String signatureCode = signatureCode1;
            ILogicalTable expressionTable = tableBody.getSubtable(w - 2, i, 1, 1);
            int d = expressionTable.getSource().getCell(0, 0).getHeight();
            final int z = i;
            syntaxNodeExceptionCollector.run(new Runnable() {
                
                @Override
                public void run() throws Exception {
                    IOpenMethodHeader header = OpenLManager.makeMethodHeader(getOpenl(),
                        new org.openl.source.impl.StringSourceCodeModule(
                            JavaOpenClass.VOID.getName() + " " + RandomStringUtils
                                .random(16, true, false) + "(" + signatureCode + ")",
                            null),
                        cxt);
                    int j = 0;

                    List<IParameterDeclaration> localParameterDeclarations = new ArrayList<>();
                    List<IParameterDeclaration> parameterDeclarations = new ArrayList<>();
                    if (w == 4 || mandatoryParameterDeclarationColumn) {
                        while (j < d) {
                            ILogicalTable pCodeTable = tableBody.getSubtable(w - 3, z + j, 1, 1);
                            GridCellSourceCodeModule pGridCellSourceCodeModule = new GridCellSourceCodeModule(
                                pCodeTable.getSource(),
                                cxt);
                            IParameterDeclaration parameterDeclaration = getParameterDeclaration(
                                pGridCellSourceCodeModule,
                                cxt);
                            if (parameterDeclaration != null) {
                                localParameterDeclarations.add(parameterDeclaration);
                            }
                            parameterDeclarations.add(parameterDeclaration);
                            j = j + pCodeTable.getCell(0, 0).getHeight();
                        }
                    }
                    j = 0;
                    int k = 0;
                    String[] parameterDescriptions = new String[parameterDeclarations.size()];
                    while (j < d) {
                        ILogicalTable tCodeTable = tableBody.getSubtable(0, z + j, 1, 1);
                        String parameterDescription = tCodeTable.getCell(0, 0).getStringValue();
                        if (StringUtils.isEmpty(parameterDescription)) { 
                            GridCellSourceCodeModule tGridCellSourceCodeModule = new GridCellSourceCodeModule(
                                tCodeTable.getSource(),
                                cxt); 
                            throw SyntaxNodeExceptionUtils.createError("Description cell can't be empty", tGridCellSourceCodeModule);
                        }
                        parameterDescriptions[k++] = parameterDescription;
                        j = j + tCodeTable.getCell(0, 0).getHeight();
                    }

                    IMethodSignature newSignature = ((MethodSignature) header.getSignature())
                        .merge(localParameterDeclarations.toArray(new IParameterDeclaration[] {}));

                    GridCellSourceCodeModule expressionCellSourceCodeModule = new GridCellSourceCodeModule(
                        expressionTable.getSource(),
                        cxt);

                    CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(getOpenl(),
                        expressionCellSourceCodeModule,
                        header.getName(),
                        newSignature,
                        getXlsModuleOpenClass(),
                        cxt);
                    
                    createAndAddDefinition(parameterDescriptions, parameterDeclarations.toArray(new IParameterDeclaration[] {}), header, compositeMethod);
                }
            });

            i = i + d;
        }
        syntaxNodeExceptionCollector.throwIfAny();
    }

}

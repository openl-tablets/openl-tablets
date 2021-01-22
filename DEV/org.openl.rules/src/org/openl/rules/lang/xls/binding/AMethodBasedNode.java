package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.AliasWrapperLogic;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.MessageUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode implements IMemberBoundNode {

    private final OpenL openl;
    private final IOpenMethodHeader header;
    private ExecutableRulesMethod method;
    private final ModuleOpenClass module;

    public AMethodBasedNode(TableSyntaxNode methodNode, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(methodNode);
        this.header = header;
        this.openl = openl;
        this.module = module;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public ExecutableRulesMethod getMethod() {
        return method;
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException("Should not be called");
    }

    @Override
    public IOpenClass getType() {
        return header.getType();
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        method = createMethodShell();
        method.setModuleName(module.getModuleName());
        openClass.addMethod(method);
        getTableSyntaxNode().setMember(method);
        if (hasAliasName()) {
            openClass.addMethod(getAliasMethod(method));
        }
    }

    /**
     * Is method has an "id" property that will be used to generate additional method with name specified in property
     * suitable for direct call of rule avoiding the method dispatching mechanism.
     *
     * @return <code>true</code> if "id" property is specified.
     */
    protected boolean hasAliasName() {
        return StringUtils.isNotBlank(getTableSyntaxNode().getTableProperties().getId());
    }

    protected IOpenMethod getAliasMethod(ExecutableRulesMethod originalMethod) {
        final String aliasMethodName = getTableSyntaxNode().getTableProperties().getId();
        return AliasWrapperLogic.wrapOpenMethod(originalMethod, aliasMethodName);
    }

    protected abstract ExecutableRulesMethod createMethodShell();

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode() && header instanceof OpenMethodHeader) {
            OpenMethodHeader tableHeader = (OpenMethodHeader) header;
            tableHeader.setTypeLocation(null);
            tableHeader.setParamTypeLocations(null);
        }
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        if (header instanceof OpenMethodHeader) {
            // Validate that there are no errors in dependent types.
            OpenMethodHeader tableHeader = (OpenMethodHeader) header;

            IOpenSourceCodeModule headerSyntaxNode = null;
            // Return type
            IOpenClass type = OpenClassUtils.getRootComponentClass(tableHeader.getType());
            if (!NullOpenClass.isAnyNull(type) && type.getInstanceClass() == null) {
                headerSyntaxNode = getHeaderSyntaxNode(bindingContext);
                addTypeError(bindingContext, type, tableHeader.getTypeLocation(), headerSyntaxNode);
            }

            // Input parameters
            ILocation[] paramTypeLocations = tableHeader.getParamTypeLocations();
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                IOpenClass parameterType = OpenClassUtils
                    .getRootComponentClass(header.getSignature().getParameterType(i));

                ILocation sourceLocation = paramTypeLocations == null ? null : paramTypeLocations[i];
                if (!NullOpenClass.isAnyNull(parameterType) && parameterType.getInstanceClass() == null) {
                    if (headerSyntaxNode == null) {
                        headerSyntaxNode = getHeaderSyntaxNode(bindingContext);
                    }
                    addTypeError(bindingContext, parameterType, sourceLocation, headerSyntaxNode);
                }
            }
        }
    }

    private IOpenSourceCodeModule getHeaderSyntaxNode(IBindingContext bindingContext) {
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(getTableSyntaxNode().getGridTable(), bindingContext);

        int startPosition = getSignatureStartIndex();
        return new SubTextSourceCodeModule(src, startPosition, src.getCode().length());
    }

    protected void addTypeError(IBindingContext bindingContext,
            IOpenClass type,
            ILocation location,
            IOpenSourceCodeModule syntaxNode) {
        String message = MessageUtils.getTypeDefinedErrorMessage(type.getName());
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, null, location, syntaxNode);
        bindingContext.addError(error);
    }

    public int getSignatureStartIndex() {
        ICell cell = getTableSyntaxNode().getGridTable().getCell(0, 0);
        TextInfo tableHeaderText = new TextInfo(cell.getStringValue());
        return getTableSyntaxNode().getHeader()
            .getHeaderToken()
            .getLocation()
            .getEnd()
            .getAbsolutePosition(tableHeaderText);
    }
}

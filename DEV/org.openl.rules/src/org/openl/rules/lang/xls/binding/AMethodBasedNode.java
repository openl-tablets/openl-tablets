package org.openl.rules.lang.xls.binding;

import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.MessageUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode implements IMemberBoundNode {

    private OpenL openl;
    private IOpenMethodHeader header;
    private ExecutableRulesMethod method;
    private ModuleOpenClass module;

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
        openClass.addMethod(method);
        getTableSyntaxNode().setMember(method);
        if (hasServiceName()) {
            openClass.addMethod(getServiceMethod(method));
        }
    }

    /**
     * Is method has an "id" property that will be used to generate additional method with name specified in property
     * sutable for direct call of rule avoiding the method dispatching mechanism.
     *
     * @return <code>true</code> if "id" property is specified.
     */
    protected boolean hasServiceName() {
        return StringUtils.isNotBlank(getTableSyntaxNode().getTableProperties().getId());
    }

    protected IOpenMethod getServiceMethod(ExecutableRulesMethod originalMethod) {
        final String serviceMethodName = getTableSyntaxNode().getTableProperties().getId();
        return new AMethodBasedNodeServiceMethod(originalMethod, serviceMethodName);
    }

    private static final class AMethodBasedNodeServiceMethod extends MethodDelegator implements IMemberMetaInfo {
        private String serviceMethodName;

        public AMethodBasedNodeServiceMethod(ExecutableRulesMethod originalMethod, String serviceMethodName) {
            super(originalMethod);
            this.serviceMethodName = serviceMethodName;
        }

        @Override
        public String getName() {
            return serviceMethodName;
        }

        @Override
        public String getDisplayName(int mode) {
            return serviceMethodName;
        }

        @Override
        public BindingDependencies getDependencies() {
            return ((ExecutableRulesMethod) methodCaller).getDependencies();
        }

        @Override
        public ISyntaxNode getSyntaxNode() {
            return ((ExecutableRulesMethod) methodCaller).getSyntaxNode();
        }

        @Override
        public Map<String, Object> getProperties() {
            return ((ExecutableRulesMethod) methodCaller).getProperties();
        }

        @Override
        public String getSourceUrl() {
            return ((ExecutableRulesMethod) methodCaller).getSourceUrl();
        }
    }

    protected abstract ExecutableRulesMethod createMethodShell();

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            getMethod().setBoundNode(null);
            getMethod().getMethodProperties().setModulePropertiesTableSyntaxNode(null);
            getMethod().getMethodProperties().setCategoryPropertiesTableSyntaxNode(null);
            getMethod().getMethodProperties().setPropertiesSection(null);

            if (header instanceof OpenMethodHeader) {
                OpenMethodHeader tableHeader = (OpenMethodHeader) header;
                tableHeader.setTypeLocation(null);
                tableHeader.setParamTypeLocations(null);
            }
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
            if (type.getInstanceClass() == null) {
                headerSyntaxNode = getHeaderSyntaxNode(bindingContext);
                addTypeError(bindingContext, type, tableHeader.getTypeLocation(), headerSyntaxNode);
            }

            // Input parameters
            ILocation[] paramTypeLocations = tableHeader.getParamTypeLocations();
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                IOpenClass parameterType = OpenClassUtils.getRootComponentClass(header.getSignature().getParameterType(i));

                ILocation sourceLocation = paramTypeLocations == null ? null : paramTypeLocations[i];
                if (parameterType.getInstanceClass() == null) {
                    if (headerSyntaxNode == null) {
                        headerSyntaxNode = getHeaderSyntaxNode(bindingContext);
                    }
                    addTypeError(bindingContext, parameterType, sourceLocation, headerSyntaxNode);
                }
            }
        }
    }

    private IOpenSourceCodeModule getHeaderSyntaxNode(IBindingContext bindingContext) {
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(getTableSyntaxNode().getGridTable(),
                bindingContext);

        int startPosition = getSignatureStartIndex();
        return new SubTextSourceCodeModule(src,
                startPosition,
                src.getCode().length());
    }

    protected void addTypeError(IBindingContext bindingContext,
            IOpenClass type,
            ILocation location,
            IOpenSourceCodeModule syntaxNode) {
        String message = MessageUtils.getTypeDefinedErrorMessage(type.getName());
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, null, location, syntaxNode);
        getTableSyntaxNode().addError(error);
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

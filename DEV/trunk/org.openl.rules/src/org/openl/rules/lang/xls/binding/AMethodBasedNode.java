package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode implements IMemberBoundNode {

    private OpenL openl;
    private IOpenMethodHeader header;
    private IOpenMethod method;
    private ModuleOpenClass module;

    public AMethodBasedNode(TableSyntaxNode methodNode, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(methodNode, new IBoundNode[0]);
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

    public IOpenMethod getMethod() {
        return method;
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return false;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        throw new UnsupportedOperationException("Should not be called");
    }

    public IOpenClass getType() {
        return header.getType();
    }

    public void addTo(ModuleOpenClass openClass) {

        method = createMethodShell();
        try{
            openClass.addMethod(method);
        }catch (DuplicatedMethodException e) {
            SyntaxNodeException error = new SyntaxNodeException(null, e, getTableSyntaxNode());
            getTableSyntaxNode().addError(error);
            OpenLMessagesUtils.addError(error);
        }
        getTableSyntaxNode().setMember(method);
    }

    protected abstract IOpenMethod createMethodShell();

}

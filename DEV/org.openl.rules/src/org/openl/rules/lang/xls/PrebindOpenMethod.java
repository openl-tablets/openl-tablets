package org.openl.rules.lang.xls;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class PrebindOpenMethod extends AMethod {
    
    TableSyntaxNode tableSyntaxNode;
    
    Invokable invokable = null;
    
    public PrebindOpenMethod(IOpenMethodHeader header, TableSyntaxNode tableSyntaxNode) {
        super(header);
        this.tableSyntaxNode = tableSyntaxNode;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return invokable.invoke(target, params, env);
    }
    
    public TableSyntaxNode getSyntaxNode() {
        return tableSyntaxNode;
    }

    public void setInvokable(Invokable invokable) {
        this.invokable = invokable;
    }
}

package org.openl.rules.lang.xls;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class PrebindOpenMethod extends AMethod {
    
    TableSyntaxNode tableSyntaxNode;
    
    IMethodCaller methodCaller = null;
    
    public PrebindOpenMethod(IOpenMethodHeader header, TableSyntaxNode tableSyntaxNode) {
        super(header);
        this.tableSyntaxNode = tableSyntaxNode;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return methodCaller.invoke(target, params, env);
    }
    
    public TableSyntaxNode getSyntaxNode() {
        return tableSyntaxNode;
    }
    
    @Override
    public IOpenClass getType() {
        if (methodCaller != null){
            return methodCaller.getMethod().getType();
        }else{
            return super.getType();
        }
    }

    public void setMethodCaller(IMethodCaller invokable) {
        this.methodCaller = invokable;
    }
}

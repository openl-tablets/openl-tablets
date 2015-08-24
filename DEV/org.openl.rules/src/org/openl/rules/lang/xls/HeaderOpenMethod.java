package org.openl.rules.lang.xls;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class HeaderOpenMethod extends AMethod {
    
    TableSyntaxNode tableSyntaxNode;
    
    public HeaderOpenMethod(IOpenMethodHeader header, TableSyntaxNode tableSyntaxNode) {
        super(header);
        this.tableSyntaxNode = tableSyntaxNode;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        throw new IllegalStateException("Prebinded method can't be executed!");
    }
    
    public TableSyntaxNode getSyntaxNode() {
        return tableSyntaxNode;
    }

}

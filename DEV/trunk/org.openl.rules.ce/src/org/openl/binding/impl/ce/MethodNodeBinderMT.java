package org.openl.binding.impl.ce;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodNodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

public class MethodNodeBinderMT extends MethodNodeBinder {

	@Override
	protected IBoundNode makeArraParametersMethod(ISyntaxNode methodNode,
			IBindingContext bindingContext, String methodName,
			IOpenClass[] argumentTypes, IBoundNode[] children) throws Exception {
		return new ArrayArgumentsMethodBinderMT(methodName, argumentTypes, children)
        .bind(methodNode, bindingContext);
 	
				
	}

}

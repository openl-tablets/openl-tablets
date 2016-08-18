package org.openl.binding.impl.ce;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ArrayArgumentsMethodBinder;
import org.openl.binding.impl.MethodNodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.ce.conf.ServiceMTConfiguration;
import org.openl.util.ce.impl.ServiceMT;

public class MethodNodeBinderMT extends MethodNodeBinder {

	@Override
	protected IBoundNode makeArrayParametersMethod(ISyntaxNode methodNode,
			IBindingContext bindingContext, String methodName,
			IOpenClass[] argumentTypes, IBoundNode[] children) throws Exception {

		ServiceMTConfiguration config = ServiceMT.getService().getConfig();

		if (config.isCallComponentUsingMT(methodName))
			return new ArrayArgumentsMethodBinderMT(methodName, argumentTypes,
					children).bind(methodNode, bindingContext);
		else
			return new ArrayArgumentsMethodBinder(methodName, argumentTypes,
					children).bind(methodNode, bindingContext);

	}

}

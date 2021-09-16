package org.openl.rules.binding;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

public final class RulesModuleBindingContextHelper {
    private RulesModuleBindingContextHelper() {
    }

    // If compilation happens before finalizeBind then all types in the signature must be compiled also
    public static void compileAllTypesInSignature(IMethodSignature signature, IBindingContext bindingContext) {
        // Compile all types in the signature
        for (IOpenClass paramType : signature.getParameterTypes()) {
            IOpenClass pType = paramType;
            while (pType.isArray()) {
                pType = pType.getComponentClass();
            }
            bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, pType.getName());
        }
    }
}

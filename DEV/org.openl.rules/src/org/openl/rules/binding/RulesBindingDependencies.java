package org.openl.rules.binding;

import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;

public class RulesBindingDependencies extends BindingDependencies {

    @Override
    public void addMethodDependency(IOpenMethod method, IBoundNode node) {
        getMethodsMap().put(method, node);
        // check if method is instance of Openl executable rules method.
        if (method instanceof ExecutableMethod) {
            getRulesMethodsMap().put((ExecutableMethod) method, node);
        } else if (method instanceof OpenMethodDispatcher) {
            List<IOpenMethod> overlappedMethods = ((OpenMethodDispatcher) method).getCandidates();
            for (IOpenMethod overlappedMethod : overlappedMethods) {
                addMethodDependency(overlappedMethod, node);
            }

        }
    }

}

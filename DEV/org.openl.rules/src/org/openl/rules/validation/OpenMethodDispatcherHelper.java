package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

class OpenMethodDispatcherHelper {

    private OpenMethodDispatcherHelper() {
    }

    /**
     * Some of the list values from income parameters may be wrapped by {@link OpenMethodDispatcher}, extract all
     * methods from a class and return new list of methods without {@link OpenMethodDispatcher}.
     * 
     * @param openClass the class
     * @return list of {@link IOpenMethod}, unwrapped from {@link OpenMethodDispatcher}.
     */

    static List<IOpenMethod> extractMethods(IOpenClass openClass) {
        List<IOpenMethod> result = new ArrayList<>();
        extractMethods(openClass.getMethods(), result);
        return result;
    }

    private static void extractMethods(Collection<IOpenMethod> methods, List<IOpenMethod> result) {
        for (IOpenMethod method : methods) {
            if (method instanceof OpenMethodDispatcher) {
                extractMethods(((OpenMethodDispatcher) method).getCandidates(), result);
            } else {
                result.add(method);
            }
        }
    }

}

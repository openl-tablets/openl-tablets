/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.engine;

import java.util.List;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;

/**
 * Provides utility methods.
 * 
 * @author snshor
 * 
 */
public final class OpenLUtils {

    /**
     * Finds method with given name and parameters in open class.
     * 
     * @param methodName method name
     * @param paramTypes parameters types
     * @param openClass {@link IOpenClass} instance
     * @return {@link IOpenMethod} instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static IOpenMethod getMethod(final String methodName, IOpenClass[] paramTypes, IOpenClass openClass) {

        IOpenMethod method = null;

        if (paramTypes != null) {
            method = openClass.getMatchingMethod(methodName, paramTypes);
        } else {
            List<IOpenMethod> list = CollectionUtils.findAll(openClass.getMethods(),
                new CollectionUtils.Predicate<IOpenMethod>() {
                    @Override
                    public boolean evaluate(IOpenMethod method) {
                        return methodName.equals(method.getName());
                    }
                });
            if (list.size() > 1) {
                throw new AmbiguousMethodException(methodName, IOpenClass.EMPTY, list);
            } else if (list.size() == 1) {
                method = list.get(0);
            }
        }

        if (method == null) {
            throw new MethodNotFoundException("Can not run method: ",
                methodName,
                paramTypes == null ? IOpenClass.EMPTY : paramTypes);
        }

        return method;
    }

}

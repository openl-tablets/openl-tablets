/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.engine;

import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;
import org.openl.util.ISelector;

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
    @SuppressWarnings("unchecked")
    public static IOpenMethod getMethod(String methodName, IOpenClass[] paramTypes, IOpenClass openClass) {

        IOpenMethod method = null;

        if (paramTypes != null) {
            method = openClass.getMatchingMethod(methodName, paramTypes);
        } else {
            AStringConvertor<INamedThing> sc = INamedThing.NAME_CONVERTOR;
            ISelector<IOpenMethod> nameSel = new ASelector.StringValueSelector(methodName, sc);

            List<IOpenMethod> list = AOpenIterator.select(openClass.methods(), nameSel).asList();
            if (list.size() > 1) {
                throw new AmbiguousMethodException(methodName, IOpenClass.EMPTY, list);
            } else if (list.size() == 1) {
                method = list.get(0);
            }
        }

        if (method == null) {
            throw new MethodNotFoundException("Can not run method: ", methodName, paramTypes == null ? IOpenClass.EMPTY
                    : paramTypes);
        }

        return method;
    }

}

/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.Collections;
import java.util.List;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class AmbiguousMethodException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -4733490029481524664L;

    private final List<IOpenMethod> matchingMethods;

    private final String methodName;

    private final IOpenClass[] pars;

    public AmbiguousMethodException(String methodName, IOpenClass[] pars, List<IOpenMethod> matchingMethods) {
        this.methodName = methodName;
        this.pars = pars;
        this.matchingMethods = Collections.unmodifiableList(matchingMethods);
    }

    public List<IOpenMethod> getMatchingMethods() {
        return matchingMethods;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Method '");
        MethodUtil.printMethod(methodName, pars, sb);
        sb.append("' is ambiguous:\n").append("Matching methods:\n");
        for (IOpenMethod method : matchingMethods) {
            MethodUtil.printMethod(method, sb).append('\n');
        }

        return sb.toString();
    }

}

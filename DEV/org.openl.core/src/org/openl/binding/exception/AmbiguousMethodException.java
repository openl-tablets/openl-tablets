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

    private final List<IOpenMethod> matchingMethods;

    private final String methodName;

    private IOpenClass[] pars;

    public AmbiguousMethodException(String methodName, List<IOpenMethod> matchingMethods) {
        this.methodName = methodName;
        this.matchingMethods = Collections.unmodifiableList(matchingMethods);
    }

    public AmbiguousMethodException(String methodName, IOpenClass[] pars, List<IOpenMethod> matchingMethods) {
        this.methodName = methodName;
        this.pars = pars != null ? pars : IOpenClass.EMPTY;
        this.matchingMethods = Collections.unmodifiableList(matchingMethods);
    }

    public List<IOpenMethod> getMatchingMethods() {
        return matchingMethods;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Method '");
        if (pars != null) {
            MethodUtil.printMethod(methodName, pars, sb);
        } else {
            sb.append(methodName);
        }
        sb.append("' is ambiguous:\n").append("Matching methods:\n");
        for (IOpenMethod method : matchingMethods) {
            MethodUtil.printMethod(method, sb).append('\n');
        }

        return sb.toString();
    }

}

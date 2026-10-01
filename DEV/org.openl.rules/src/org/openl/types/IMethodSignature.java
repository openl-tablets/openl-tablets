/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 */
public interface IMethodSignature {

    IMethodSignature VOID = new VoidSignature();

    int getNumberOfParameters();

    String getParameterName(int i);

    IOpenClass getParameterType(int i);

    IOpenClass[] getParameterTypes();

}

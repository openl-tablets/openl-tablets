/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 *
 */
public interface IOpenMethod extends IOpenMethodHeader, IMethodCaller {
    IOpenMethod[] EMPTY_ARRAY = new IOpenMethod[0];

    boolean isConstructor();
}

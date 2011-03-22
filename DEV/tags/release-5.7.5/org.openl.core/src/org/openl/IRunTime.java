/*
 * Created on Aug 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

/**
 * @author snshor
 *
 */
public interface IRunTime {
    void extend(IRunTime irt);

    IOpenVM getVM();
}

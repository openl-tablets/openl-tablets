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
public interface ICompileTime {
    void extend(ICompileTime ict);

    IOpenBinder getBinder();

    IOpenParser getParser();

}

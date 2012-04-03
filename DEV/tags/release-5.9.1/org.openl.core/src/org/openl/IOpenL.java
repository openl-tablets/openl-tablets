/*
 * Created on Aug 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

/**
 * @author snshor
 */

public interface IOpenL {

    ICompileTime getCompileTime();

    IRunTime getRunTime();

    // //implements language extension
    // public void extend(IOpenL openl);
    //
    //
    //
    // //convinience methods
    // public Object evaluate(IOpenSourceCodeModule code) throws
    // OpenLRuntimeException;
    //
    // public IOpenParser getParser();
    //
    // public IOpenBinder getBinder();
    //
    // public IOpenVM getVM();
    //

}

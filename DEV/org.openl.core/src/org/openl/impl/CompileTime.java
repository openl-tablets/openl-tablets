/*
 * Created on Aug 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.impl;

import org.openl.ICompileTime;
import org.openl.IOpenBinder;
import org.openl.IOpenParser;

/**
 * @author snshor
 *
 */
public class CompileTime implements ICompileTime {
    IOpenParser parser;

    IOpenBinder binder;

    CompileTime delegate;

    /*
     * (non-Javadoc)
     *
     * @see org.openl.ICompileTime#extend(org.openl.ICompileTime)
     */
    public void extend(ICompileTime ict) {
        // TODO Auto-generated method stub

    }

    /**
     * @return
     */
    public IOpenBinder getBinder() {
        return binder;
    }

    /**
     * @return
     */
    public IOpenParser getParser() {
        return parser;
    }

    /**
     * @param binder
     */
    public void setBinder(IOpenBinder binder) {
        this.binder = binder;
    }

    /**
     * @param parser
     */
    public void setParser(IOpenParser parser) {
        this.parser = parser;
    }

}

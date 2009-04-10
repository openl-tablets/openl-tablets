/*
 * Created on Aug 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.impl;

import org.openl.ICompileTime;
import org.openl.IOpenBinder;
import org.openl.IOpenL;
import org.openl.IOpenParser;
import org.openl.IOpenSourceCodeModule;
import org.openl.IOpenVM;
import org.openl.IRunTime;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;

/**
 * @author snshor
 *
 */
public class OpenLImpl implements IOpenL {
    ICompileTime compileTime;

    IRunTime runTime;

    public Object evaluate(IOpenSourceCodeModule code) throws OpenLRuntimeException {
        IParsedCode pc = getParser().parseAsMethodBody(code);
        ISyntaxError[] error = pc.getError();
        if (error.length > 0) {
            throw new SyntaxErrorException("Parsing Error:", error);
        }

        IBoundCode bc = getBinder().bind(pc);
        error = bc.getError();
        if (error.length > 0) {
            throw new SyntaxErrorException("Binding Error:", error);
        }
        return getVM().getRunner().run((IBoundMethodNode) bc.getTopNode(), new Object[0]);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenL#extend(org.openl.IOpenL)
     */
    public void extend(IOpenL openl) {
        if (openl == null) {
            return;
        }
        if (compileTime == null) {
            compileTime = openl.getCompileTime();
        } else {
            compileTime.extend(openl.getCompileTime());
        }

    }

    public IOpenBinder getBinder() {
        return getCompileTime().getBinder();
    }

    /**
     * @return
     */
    public ICompileTime getCompileTime() {
        return compileTime;
    }

    public IOpenParser getParser() {
        return getCompileTime().getParser();
    }

    /**
     * @return
     */
    public IRunTime getRunTime() {
        return runTime;
    }

    public IOpenVM getVM() {
        return getRunTime().getVM();
    }

    /**
     * @param time
     */
    public void setCompileTime(ICompileTime time) {
        compileTime = time;
    }

    /**
     * @param time
     */
    public void setRunTime(IRunTime time) {
        runTime = time;
    }

}

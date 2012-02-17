/*
 * Created on Apr 12, 2004
 *
 */
package org.openl.rules.lang.xls.main;

import org.openl.main.Engine;

/**
 * @deprecated
 * 
 * @author Stanislav Shor
 *
 */
public class XlsEngine extends Engine {

    /**
     * @param openlName
     * @param fileName
     * @param methodName
     */
    public XlsEngine(String fileName, String methodName) {
        super("org.openl.rules.lang.xls", fileName, methodName);
    }
}

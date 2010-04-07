/**
 * Created Dec 5, 2006
 */
package org.openl.rules.test;

import org.openl.OpenL;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.engine.OpenLManager;
import org.openl.engine.OpenLUtils;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 * 
 */
public class Tools {

    static public OpenL getOpenL() {
        OpenL openl = OpenL.getInstance("org.openl.xls");
        return openl;
    }

    static public XlsModuleOpenClass createModule(String fname) {

        OpenL openl = getOpenL();

        XlsModuleOpenClass xmo = (XlsModuleOpenClass) OpenLManager.compileModule(openl, new FileSourceCodeModule(fname, null));
        return xmo;
    }

    static public Object run(String file, String methodName, Object[] params) throws MethodNotFoundException, OpenLRuntimeException, SyntaxNodeException {
        Object res = OpenLManager.runMethod(getOpenL(), new FileSourceCodeModule(file, null), methodName, null, params);
        return res;
    }

}

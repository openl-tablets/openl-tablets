/**
 * Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class TestMethodHelper {
    /** Field name for the expected result in test*/
    public static final String EXPECTED_RESULT_NAME = "_res_";
    
    /** Field name for the expecting eror in test*/
    public static final String EXPECTED_ERROR = "_error_";
    
    /** Field name for defining runtime context in test*/
    public static final String CONTEXT_NAME = "_context_";
    
    /** Field name for test unit description in test*/ 
    public static final String DESCRIPTION_NAME = "_description_";

    public static IOpenMethodHeader makeHeader(String tableName, XlsModuleOpenClass module) {

        return new OpenMethodHeader(tableName,
            JavaOpenClass.getOpenClass(TestUnitsResults.class),
            IMethodSignature.VOID,
            module);
    }

}

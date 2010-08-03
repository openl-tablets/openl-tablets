/**
 * Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class TestMethodHelper {

    public static final String EXPECTED_RESULT_NAME = "_res_";
    public static final String EXPECTED_ERROR = "_error_";
    public static final String CONTEXT_NAME = "_context_";
    public static final String DESCRIPTION_NAME = "_description_";

    public static IOpenMethodHeader makeHeader(String tableName, IOpenMethod testedMethod) {

        return new OpenMethodHeader(tableName + "TestAll",
            JavaOpenClass.getOpenClass(TestResult.class),
            IMethodSignature.VOID,
            testedMethod.getDeclaringClass());
    }

}

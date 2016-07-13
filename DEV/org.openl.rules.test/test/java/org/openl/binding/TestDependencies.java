/**
 * Created Dec 3, 2006
 */
package org.openl.binding;

import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;

/**
 * @author snshor
 */
public class TestDependencies extends TestCase {

    private static final String FILE_NAME = "org/openl/binding/TestBinding.xls";

    public void testDependencies() throws URISyntaxException {

        XlsModuleOpenClass xmo = _createModule();

        for (IOpenMethod m : xmo.getMethods()) {
            BindingDependencies bd = new BindingDependencies();

            if (m instanceof IDecisionTable) {
                IDecisionTable dt = (IDecisionTable) m;
                dt.updateDependency(bd);

            } else if (m instanceof CompositeMethod) {
                CompositeMethod cm = (CompositeMethod) m;
                cm.updateDependency(bd);

            } else {
                System.out.println("Method " + m.getName() + " has type " + m.getClass());
                continue;
            }

            System.out.println();
            System.out.println(m.getName());
            System.out.println(bd);
        }
    }

    private XlsModuleOpenClass _createModule() throws URISyntaxException {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);

        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_RULE_NAME);

        XlsModuleOpenClass xmo = (XlsModuleOpenClass) OpenLManager.compileModule(openl, new FileSourceCodeModule(url.toURI().getPath(), null));
        return xmo;
    }
}

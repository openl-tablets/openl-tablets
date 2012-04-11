/**
 * Created Dec 3, 2006
 */
package org.openl.binding;

import java.net.URL;

import junit.framework.TestCase;

import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.test.Tools;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.Tracer;

/**
 * @author snshor
 * 
 */
public class TestDependencies extends TestCase {

    private static final String FILE_NAME = "org/openl/binding/TestBinding.xls";

    public void testDependencies() {

        XlsModuleOpenClass xmo = _createModule();

        for (IOpenMethod m : xmo.getMethods()) {
            BindingDependencies bd = new BindingDependencies();

            if (m instanceof DecisionTable) {
                DecisionTable dt = (DecisionTable) m;
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

    public void testTracer() throws Exception {

        Tracer t = new Tracer();
        Tracer.setTracer(t);

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        
        Object res = Tools.run(url.getPath(), "hello1", new Object[] { new Integer(23) });
        System.out.println(res);

        ITracerObject[] tt = t.getTracerObjects();

        for (int i = 0; i < tt.length; i++) {
            printTO(tt[i], 0);
        }

    }

    void printTO(ITracerObject to, int level) {
        
        for (int i = 0; i < level * 2; i++) {
            System.out.print(' ');
        }

        System.out.println("TRACE: " + to.getDisplayName(INamedThing.REGULAR));
        System.out.println(SourceCodeURLConstants.AT_PREFIX + to.getUri());

        ITracerObject[] tt = to.getTracerObjects();

        for (int i = 0; i < tt.length; i++) {
            printTO(tt[i], level + 1);
        }
    }

    private XlsModuleOpenClass _createModule() {

        URL url = this.getClass().getClassLoader().getResource(FILE_NAME);
        return Tools.createModule(url.getPath());
    }
}

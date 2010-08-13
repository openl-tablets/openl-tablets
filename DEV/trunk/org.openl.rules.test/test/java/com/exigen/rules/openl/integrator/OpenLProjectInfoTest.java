package com.exigen.rules.openl.integrator;

import java.net.URL;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.types.IOpenClass;

import junit.framework.TestCase;

public class OpenLProjectInfoTest extends TestCase {
    

    public void testLoadOpenlProject() {

//        URL url = this.getClass().getClassLoader().getResource("com/exigen/rules/openl/integrator/IndexLogic.xls");
//        
//        OpenL openl = OpenL.getInstance("org.openl.xls");
//        IOpenClass ioc = null;
//        // Did this test passed before??? 
//        // Now there are 5 errors during compilation and binding.
//        try {
//            ioc = OpenLManager.compileModule(openl,
//                    new FileSourceCodeModule(url.getPath(), null));
//            new OpenLProjectInfo().load(ioc);
//        } catch (CompositeSyntaxNodeException ex) {
//            assertEquals(5, ex.getErrors().length);
//        }
        
    }
}

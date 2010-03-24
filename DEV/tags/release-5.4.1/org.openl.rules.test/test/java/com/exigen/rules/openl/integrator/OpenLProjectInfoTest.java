package com.exigen.rules.openl.integrator;

import java.net.URL;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;

import junit.framework.TestCase;

public class OpenLProjectInfoTest extends TestCase {
    

    public void testLoadOpenlProject() {

        URL url = this.getClass().getClassLoader().getResource("com/exigen/rules/openl/integrator/IndexLogic.xls");
        
        OpenL openl = OpenL.getInstance("org.openl.xls");
        IOpenClass ioc = OpenLManager.compileModule(openl,
                new FileSourceCodeModule(url.getPath(), null));

        new OpenLProjectInfo().load(ioc);
    }
}

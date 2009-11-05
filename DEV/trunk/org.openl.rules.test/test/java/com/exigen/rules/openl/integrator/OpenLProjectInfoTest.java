package com.exigen.rules.openl.integrator;

import java.net.URL;

import org.openl.OpenL;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;

import junit.framework.TestCase;

public class OpenLProjectInfoTest extends TestCase {
    

    public void testLoadOpenlProject() {

        URL url = this.getClass().getClassLoader().getResource("com/exigen/rules/openl/integrator/IndexLogic.xls");
        
        IOpenClass ioc = OpenL.getInstance("org.openl.xls").compileModule(
                new FileSourceCodeModule(url.getPath(), null));

        new OpenLProjectInfo().load(ioc);
    }
}

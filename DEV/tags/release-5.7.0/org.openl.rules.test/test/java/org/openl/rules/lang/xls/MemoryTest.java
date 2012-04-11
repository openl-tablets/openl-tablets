/*
 * Created on Dec 30, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.net.URL;
import java.util.HashMap;

import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.main.OpenlMain;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class MemoryTest extends TestCase {

    public void testMemory1() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/lang/xls/Test2.xls");
        
        for (int i = 0; i < 100; i++) {
            System.out.println("############### " + i + "   ##########");

            OpenL.reset();
            OpenLConfiguration.reset();
            HashMap<?, ClassLoader> old = ClassLoaderFactory.reset();
            JavaOpenClass.resetAllClassloaders(old);

            System.out.println("############### " + "GC" + "   ##########");
            System.gc();

            new OpenlMain("org.openl.xls").safeRunOpenl("org.openl.xls", new FileSourceCodeModule(url.getPath(), null),
                    "hello", new Object[] { new Integer(10) });
        }

        JavaOpenClass.printCache();

    }

    public void testMemory2() throws Exception {
        
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/lang/xls/Test2.xls");
        
        for (int i = 0; i < 20; i++) {
        
            System.out.println("############### " + i + "   ##########");

            OpenL.reset();
            OpenLConfiguration.reset();
            ClassLoaderFactory.reset();
            System.out.println("############### " + "GC" + "   ##########");
            System.gc();

            new OpenlMain("org.openl.rules.lang.xls").safeRunOpenl("org.openl.rules.lang.xls",
                    new FileSourceCodeModule(url.getPath(), url.getPath()), "hello", new Object[] { new Integer(10) });
        }

// What is this ?
//        System.out.println("Sleeping ...");
//        Thread.sleep(10000000);
//        System.out.println("Finished ...");

    }
}

/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

/**
 * @author snshor
 *
 */
public class JavaLang extends JavaImportTypeLibrary {
    public JavaLang() {
        super(new String[] { "java.lang" }, null, ClassLoader.getSystemClassLoader());
    }

}

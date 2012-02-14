/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Arrays;

/**
 * @author snshor
 *
 */
public class JavaLang extends JavaImportTypeLibrary {
    public JavaLang() {        
        super(null, Arrays.asList((new String[] { "java.lang" })), ClassLoader.getSystemClassLoader());
    }

}

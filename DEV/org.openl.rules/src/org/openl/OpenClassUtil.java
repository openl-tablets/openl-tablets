package org.openl;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.types.java.JavaOpenClass;

public class OpenClassUtil {

    public static void release(CompiledOpenClass compiledOpenClass) {
        if (compiledOpenClass != null) {
            releaseClassLoader(compiledOpenClass.getClassLoader());
        }
    }

    public static void releaseClassLoader(ClassLoader classloader) {
        if (classloader != null) {
            JavaOpenClass.resetClassloader(classloader);
            String2DataConvertorFactory.unregisterClassLoader(classloader);
            ClassLoaderUtils.close(classloader);
        }
    }
}

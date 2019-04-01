/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.net.URL;

/**
 * @author snshor
 *
 */
public interface IConfigurableResourceContext {

    Class<?> findClass(String className);

    URL findClassPathResource(String url);

    File findFileSystemResource(String url);

    String findProperty(String propertyName);

    ClassLoader getClassLoader();

    IOpenLConfiguration getConfiguration();
}

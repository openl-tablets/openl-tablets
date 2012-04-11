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
    public Class<?> findClass(String className);

    public URL findClassPathResource(String url);

    public File findFileSystemResource(String url);

    public String findProperty(String propertyName);

    public ClassLoader getClassLoader();

    IOpenLConfiguration getConfiguration();

}

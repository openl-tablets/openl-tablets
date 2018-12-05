/*
 * Created on Jul 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

import org.openl.types.IOpenSchema;
import org.openl.types.impl.AOpenFactory;

/**
 * @author snshor
 *
 */
public class JavaOpenFactory extends AOpenFactory {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.impl.AOpenFactory#loadSchema(java.lang.String)
     */
    @Override
    public IOpenSchema loadSchema(String uri) throws Exception {
        // TODO right now we think it's filesystem-based classpath, we should
        // probably change it later
        return loadSchemaUsingClasspath(splitClassPath(uri));
    }

    IOpenSchema loadSchemaUsingClasspath(String[] classpath) throws Exception {
        URL[] urls = new URL[classpath.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = new File(classpath[i]).getCanonicalFile().toURI().toURL();
        }

        return new JavaOpenSchema(classpath, new URLClassLoader(urls));
    }

    protected String[] splitClassPath(String classpath) {
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

        String[] res = new String[st.countTokens()];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

}

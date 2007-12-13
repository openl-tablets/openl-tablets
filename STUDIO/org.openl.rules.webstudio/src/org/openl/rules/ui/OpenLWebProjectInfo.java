package org.openl.rules.ui;

import org.openl.main.OpenLProjectPropertiesLoader;

import org.openl.util.StringTool;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Properties;


/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class OpenLWebProjectInfo {
    String workspace;
    String name;
    Properties projectProperties;
    URLClassLoader ucl;

/**
         * @param workspace
         * @param name
         */
    public OpenLWebProjectInfo(String workspace, String name) {
        this.workspace = workspace;
        this.name = name;
    }

    public String projectHome() {
        return workspace + "/" + name;
    }

    public String projectClasspath() {
        return new OpenLProjectPropertiesLoader().loadExistingClasspath(projectHome());
    }

    public synchronized Properties getProjectProperties() {
        if (projectProperties == null) {
            projectProperties = new OpenLProjectPropertiesLoader().loadProjectProperties(projectHome());
        }
        return projectProperties;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the workspace.
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * DOCUMENT ME!
     *
     * @param workspace The workspace to set.
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * DOCUMENT ME!
     *
     * @param parent
     * @param reload DOCUMENT ME!
     *
     * @return
     *
     * @throws IOException
     */
    public ClassLoader getClassLoader(ClassLoader parent, boolean reload)
        throws IOException
    {
        if ((ucl != null) && !reload) {
            return ucl;
        }
        String classpath = projectClasspath();

        String[] files = StringTool.tokenize(classpath, File.pathSeparator);

        URL[] urls = new URL[files.length];

        for (int i = 0; i < files.length; i++) {
            File f = new File(files[i]);

            if (f.isAbsolute()) {
                urls[i] = new File(files[i]).toURL();
            } else {
                urls[i] = new File(projectHome(), files[i]).toURL();
            }
        }

        // adds parent class loader to reach context dependent jars/classes
        ucl = new URLClassLoader(urls, parent);
        return ucl;
    }

    /**
     * DOCUMENT ME!
     *
     * @param wrapperClassName
     *
     * @return
     */
    public String getDisplayName(String wrapperClassName) {
        Properties p = getProjectProperties();
        if (p == null) {
            return wrapperClassName;
        }
        return p.getProperty(wrapperClassName
            + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, wrapperClassName);
    }

    /**
     *
     */
    public void reset() {
        projectProperties = null;
    }
}

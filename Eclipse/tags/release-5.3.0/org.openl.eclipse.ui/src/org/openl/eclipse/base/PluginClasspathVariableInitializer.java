/*
 * Created on 13.12.2004
 */
package org.openl.eclipse.base;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;

/**
 * @author smesh
 */
public class PluginClasspathVariableInitializer extends ClasspathVariableInitializer {

    static class PluginVariable {

        private String pluginId;

        private String name;

        public PluginVariable(String pluginId, String name) {
            this.pluginId = pluginId;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getPluginId() {
            return pluginId;
        }

        @Override
        public String toString() {
            return "pluginId=" + pluginId + " varName=" + name;
        }
    }

    static IPath getPluginHome(String pluginId) throws Exception {
        Bundle bundle = Platform.getBundle(pluginId);

        if (bundle == null) {
            throw new Exception("Plugin not found: " + pluginId);
        }

        URL localUrl = FileLocator.toFileURL(bundle.getEntry("/"));
        String fullPath = new File(localUrl.getPath()).getAbsolutePath();

        return new Path(fullPath);
    }

    public PluginVariable decodeVariable(String encodedVariable) throws Exception {

        String pluginId;
        String varName;

        int i = encodedVariable.lastIndexOf('_');

        if (i < 0) {
            pluginId = encodedVariable;
            varName = "";
        } else {

            pluginId = encodedVariable.substring(0, i);
            varName = encodedVariable.substring(i + 1);
        }

        PluginVariable result = new PluginVariable(pluginId, varName);
        return result;
    }

    protected IProgressMonitor getProgressMonitor() {
        return null;
    }

    protected IPath getVariableValue(String variable) throws Exception {
        PluginVariable pluginVariable = decodeVariable(variable);

        if ("HOME".equalsIgnoreCase(pluginVariable.getName())) {
            return getPluginHome(pluginVariable.getPluginId());
        }

        throw new Exception("Invalid plugin variable: " + pluginVariable);

    }

    /**
     * @see ClasspathVariableInitializer#initialize(String)
     */
    @Override
    public void initialize(String variable) {
        try {
            IPath value = getVariableValue(variable);

            if (value != null) {
                JavaCore.setClasspathVariable(variable, value, getProgressMonitor());
            } else {
                JavaCore.removeClasspathVariable(variable, getProgressMonitor());
            }

        } catch (Throwable t) {
            System.err.println("Exception is ClasspathVariableInitializerProxy.initialize(" + variable + "): "
                    + t.getMessage());
        }
    }

}
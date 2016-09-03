package org.openl.rules.project.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.openl.classloader.ClassLoaderCloserFactory;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDescriptor {
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptor.class);
    private String id;
    private String name;
    private String comment;
    private File projectFolder;
    private List<Module> modules;
    private List<PathEntry> classpath;
    private List<Property> properties;
    
    private List<ProjectDependencyDescriptor> dependencies;
    private ClassLoader classLoader;
    private String propertiesFileNamePattern;
    private String propertiesFileNameProcessor;

    public String getPropertiesFileNamePattern() {
        return propertiesFileNamePattern;
    }

    public void setPropertiesFileNamePattern(String propertiesFileNamePattern) {
        this.propertiesFileNamePattern = propertiesFileNamePattern;
    }

    public String getPropertiesFileNameProcessor() {
        return propertiesFileNameProcessor;
    }

    public void setPropertiesFileNameProcessor(String propertiesFileNameProcessor) {
        this.propertiesFileNameProcessor = propertiesFileNameProcessor;
    }

    public List<ProjectDependencyDescriptor> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ProjectDependencyDescriptor> dependencies) {
        this.dependencies = dependencies;
    }

    public File getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(File projectRoot) {
        this.projectFolder = projectRoot;
    }

    /**
     * @deprecated Id isn't used anymore. Use {@link #getName()} instead.
     */
    @Deprecated
    public String getId() {
        return id;
    }

    /**
     * @deprecated Id isn't used anymore. Use {@link #setName(String)} instead.
     */
    @Deprecated
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }

    /**
     * @param reload Boolean flag that indicates whether classloader must be
     *               reloaded or used existing.
     * @return ClassLoader for this project.
     * @deprecated Must be removed to separate class. Project descriptor is just
     * description of project and must be simple java bean.
     */
    public ClassLoader getClassLoader(boolean reload) {
        if (classLoader == null || reload) {
            unregisterClassloader(classLoader);
            URL[] urls = new URL[0];
            // temporary commented. as we extends the strategies classloaders
            // with this URLS.
            // it is done to ensure that wrapper class will be loaded with
            // strategy classloader.
            // URL[] urls = getClassPathUrls();
            classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        }
        return classLoader;
    }

    public URL[] getClassPathUrls() {
        if (classpath == null) {
            return new URL[]{};
        }
        URL[] urls = new URL[classpath.size()];

        int i = 0;

        for (PathEntry entry : classpath) {
            File f = new File(entry.getPath());

            try {
                if (f.isAbsolute()) {
                    urls[i] = f.toURI().toURL();
                } else {
                    urls[i] = new File(projectFolder, entry.getPath()).toURI().toURL();
                }
                i++;
            } catch (MalformedURLException e) {
                log.error("Bad URL in classpath \"{}\"", entry.getPath());
            }
        }
        return urls;
    }

    /**
     * Class loader of current project have to be unregistered if it is not in
     * use to prevent memory leaks.
     *
     * @param classLoader ClassLoader to unregister.
     * @deprecated Must be removed to separate class. Project descriptor is just
     * description of project and must be simple java bean.
     */
    private void unregisterClassloader(ClassLoader classLoader) {
        if (classLoader != null) {
            JavaOpenClass.resetClassloader(classLoader);
            String2DataConvertorFactory.unregisterClassLoader(classLoader);
            ClassLoaderCloserFactory.getClassLoaderCloser().close(classLoader);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            // TODO Must be removed to separate class. Project descriptor is just description of project and must be simple java bean.
            unregisterClassloader(classLoader);
        } catch (Throwable ignore) {
        } finally {
            super.finalize();
        }
    }

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
}

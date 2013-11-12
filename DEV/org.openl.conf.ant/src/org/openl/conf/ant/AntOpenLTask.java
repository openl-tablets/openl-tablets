/*
 *  Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.openl.conf.ClassFactory;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NodeBinderFactoryConfiguration;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.OpenFactoryConfiguration;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.conf.UserContext;
import org.openl.syntax.grammar.IGrammar;

/**
 * @author snshor
 * 
 */
public class AntOpenLTask extends Task {

    static public IOpenLConfiguration lastConfiguration;

    boolean shared = false;

    OpenLConfiguration conf = new OpenLConfiguration();

    String category;
    String classpath;

    String extendsCategory;

    static IOpenLConfiguration retrieveConfiguration() {
        if (lastConfiguration == null) {
            throw new NullPointerException();
        }
        IOpenLConfiguration ret = lastConfiguration;
        lastConfiguration = null;
        return ret;
    }

    public void addConfiguredTypeFactory(OpenFactoryConfiguration of) {
        conf.addOpenFactory(of);
    }

    public NodeBinderFactoryConfiguration createBindings() {
        NodeBinderFactoryConfiguration nbf = new NodeBinderFactoryConfiguration();
        conf.setBinderFactory(nbf);
        return nbf;
    }

    public ClassFactory createGrammar() {
        ClassFactory cf = new ClassFactory();
        cf.setExtendsClassName(IGrammar.class.getName());
        conf.setGrammarFactory(cf);
        return cf;
    }

    public LibraryFactoryConfiguration createLibraries() {
        LibraryFactoryConfiguration mf = new LibraryFactoryConfiguration();
        conf.setMethodFactory(mf);
        return mf;
    }

    public TypeCastFactory createTypecast() {
        TypeCastFactory tcf = new TypeCastFactory();
        conf.setTypeCastFactory(tcf);
        return tcf;
    }

    public TypeFactoryConfiguration createTypes() {
        TypeFactoryConfiguration mf = new TypeFactoryConfiguration();
        conf.setTypeFactory(mf);
        return mf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {

        // try
        // {
        try {
            if (category == null) {
                throw new OpenConfigurationException("The category must be set", getUri(), null);
            }

            // ClassLoaderFactory.getOpenlCoreLoader();

            IOpenLConfiguration existing;
            if ((existing = OpenLConfiguration.getInstance(category, AntOpenLBuilder.userCxt.top())) != null) {
                // has been loaded and registered already
                // getProject().addReference(getCategory() + ".configuration",
                // existing);
                saveConfiguration(existing);
                return;
            }

            IOpenLConfiguration extendsConfiguration = null;
            if (extendsCategory != null) {
                if ((extendsConfiguration = OpenLConfiguration.getInstance(extendsCategory,
                        AntOpenLBuilder.userCxt.top())) == null) {
                    throw new OpenConfigurationException("The extended category " + extendsCategory
                            + " must have been loaded first", getUri(), null);
                }
            }

            IConfigurableResourceContext cxt = getConfigurationContext(extendsConfiguration);

            conf.setParent(extendsConfiguration);
            conf.setConfigurationContext(cxt);
            conf.validate(cxt);
            OpenLConfiguration.register(category, AntOpenLBuilder.userCxt.top(), conf, shared);
            // }
            // catch(Throwable t)
            // {
            // throw new BuildException(t, getLocation());
            // }
            // getProject().addReference(getCategory() + ".configuration",
            // conf);
            saveConfiguration(conf);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new BuildException(e);
        }
    }

    public String getCategory() {
        return category;
    }

    IConfigurableResourceContext getConfigurationContext(IOpenLConfiguration extendsConfiguration) throws Exception {
        ClassLoader parentLoader = extendsConfiguration == null ? ClassLoaderFactory.getOpenlCoreLoader(null)
                : extendsConfiguration.getConfigurationContext().getClassLoader();

        ClassLoader myClassLoader = parentLoader;
        if (classpath != null && classpath.trim().length() != 0) {
            String baseDir = getProject().getBaseDir().getCanonicalPath();
            UserContext ucxt = new UserContext(null, baseDir);

            myClassLoader = ClassLoaderFactory.createUserClassloader(category, classpath, parentLoader, ucxt);
        } else {
            myClassLoader = AntOpenLBuilder.userCxt.top().getUserClassLoader();
        }

        return new ConfigurableResourceContext(myClassLoader, conf);
    }

    public String getExtendsCategory() {
        return extendsCategory;
    }

    public String getUri() {
        Location loc = getLocation();
        return loc == null ? null : loc.toString();
    }

    public boolean isShared() {
        return shared;
    }

    void saveConfiguration(IOpenLConfiguration conf) {
        lastConfiguration = conf;
    }

    public void setCategory(String string) {
        category = string;
    }

    public void setClasspath(String string) {
        classpath = string;
    }

    public void setExtendsCategory(String string) {
        extendsCategory = string;
    }

    public void setShared(boolean b) {
        shared = b;
    }

}

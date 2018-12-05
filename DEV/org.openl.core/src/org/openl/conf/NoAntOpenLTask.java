package org.openl.conf;

import org.openl.syntax.grammar.IGrammar;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Allows Java based configuration without Ant
 *
 * @author snshor
 *
 */

public class NoAntOpenLTask {

    private static IOpenLConfiguration lastConfiguration;

    private boolean inheritExtendedConfigurationLoader = false;

    OpenLConfiguration conf = new OpenLConfiguration();

    private String category;

    private String extendsCategory;

    static IOpenLConfiguration retrieveConfiguration() {
        if (lastConfiguration == null) {
            throw new NullPointerException();
        }
        IOpenLConfiguration ret = lastConfiguration;
        lastConfiguration = null;
        return ret;
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
        conf.createTypeCastFactory();
        return conf.getTypeCastFactory();
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
    public void execute(IUserContext ucxt) {

        try {
            if (category == null) {
                throw new OpenConfigurationException("The category must be set", null, null);
            }
            IOpenLConfiguration existing;
            if ((existing = ucxt.getOpenLConfiguration(category)) != null) {
                saveConfiguration(existing);
                return;
            }

            IOpenLConfiguration extendsConfiguration = null;
            if (extendsCategory != null) {
                if ((extendsConfiguration = ucxt.getOpenLConfiguration(extendsCategory)) == null) {
                    throw new OpenConfigurationException("The extended category " + extendsCategory
                            + " must have been loaded first", null, null);
                }
            }

            IConfigurableResourceContext cxt = getConfigurationContext(extendsConfiguration, ucxt);

            conf.setParent(extendsConfiguration);
            conf.setConfigurationContext(cxt);
            conf.validate(cxt);
            
            ucxt.registerOpenLConfiguration(category, conf);
            
            saveConfiguration(conf);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private IConfigurableResourceContext getConfigurationContext(IOpenLConfiguration extendsConfiguration, IUserContext ucxt) {
        ClassLoader parentLoader = extendsConfiguration == null ? ClassLoaderFactory.getOpenlCoreClassLoader(null)
                : extendsConfiguration.getConfigurationContext().getClassLoader();

        if (!inheritExtendedConfigurationLoader) {
            parentLoader = ucxt.getUserClassLoader();
        }

        return new ConfigurableResourceContext(parentLoader, conf);
    }

    private void saveConfiguration(IOpenLConfiguration conf) {
        lastConfiguration = conf;
    }

    public void setCategory(String string) {
        category = string;
    }

    public void setExtendsCategory(String string) {
        extendsCategory = string;
    }

    void setInheritExtendedConfigurationLoader(boolean inheritExtendedConfigurationLoader) {
        this.inheritExtendedConfigurationLoader = inheritExtendedConfigurationLoader;
    }
}

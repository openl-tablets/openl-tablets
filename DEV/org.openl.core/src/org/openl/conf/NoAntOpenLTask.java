package org.openl.conf;

import org.openl.syntax.grammar.IGrammar;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Allows Java based configuration without Ant
 *
 * @author snshor
 */

public class NoAntOpenLTask {

    private static IOpenLConfiguration lastConfiguration;

    private final OpenLConfiguration conf = new OpenLConfiguration();
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
                throw new OpenLConfigurationException("The category must be set", null, null);
            }
            IOpenLConfiguration existing;
            if ((existing = ucxt.getOpenLConfiguration(category)) != null) {
                saveConfiguration(existing);
                return;
            }

            IOpenLConfiguration extendsConfiguration = null;
            if (extendsCategory != null) {
                if ((extendsConfiguration = ucxt.getOpenLConfiguration(extendsCategory)) == null) {
                    throw new OpenLConfigurationException(
                            "The extended category " + extendsCategory + " must have been loaded first",
                            null,
                            null);
                }
            }

            IConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader(), conf);

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

    private void saveConfiguration(IOpenLConfiguration conf) {
        lastConfiguration = conf;
    }

    public void setCategory(String string) {
        category = string;
    }

    public void setExtendsCategory(String string) {
        extendsCategory = string;
    }
}

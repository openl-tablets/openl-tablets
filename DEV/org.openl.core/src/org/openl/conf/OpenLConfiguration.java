/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.INodeBinder;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.cache.GenericKey;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.grammar.IGrammar;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenFactory;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class OpenLConfiguration implements IOpenLConfiguration {
    
    private static HashMap<Object, IOpenLConfiguration> configurations = new HashMap<Object, IOpenLConfiguration>();

    private String uri;

    private IOpenLConfiguration parent;

    private IConfigurableResourceContext configurationContext;

    private ClassFactory grammarFactory;

    private NodeBinderFactoryConfiguration binderFactory;

    private LibraryFactoryConfiguration methodFactory;
    
    private TypeCastFactory typeCastFactory;

    private TypeFactoryConfiguration typeFactory;

    private Map<String, IOpenFactoryConfiguration> openFactories = null;

    public static IOpenLConfiguration getInstance(String name, IUserContext ucxt) throws OpenConfigurationException {
        IOpenLConfiguration opc = configurations.get(name);

        if (opc != null) {
            return opc;
        }

        Object key = GenericKey.getInstance(name, ucxt);

        return configurations.get(key);

    }

    public static synchronized  void register(String name, IUserContext ucxt, IOpenLConfiguration oplc, boolean shared)
            throws OpenConfigurationException {
        Object key = null;

        if (shared) {
            key = name;
        } else {
            key = GenericKey.getInstance(name, ucxt);
        }

        IOpenLConfiguration old = configurations.get(key);
        if (old != null) {
            throw new OpenConfigurationException("The configuration " + name + " already exists", null, null);
        }
        configurations.put(key, oplc);

    }

    //FIXME: multithreading issue: users can reset foreign OpenL calculation
    public static void reset() {
        configurations = new HashMap<Object, IOpenLConfiguration>();
    }

    public static synchronized void unregister(String name, IUserContext ucxt) throws OpenConfigurationException {
        Object key = GenericKey.getInstance(name);

        // IOpenLConfiguration old =
        // (IOpenLConfiguration)configurations.get(key);
        // if (old == null)
        // {
        // throw new OpenConfigurationException("The configuration " + name + "
        // does not exists", null, null);
        // }
        configurations.remove(key);
        configurations.remove(name);

    }

    public synchronized void addOpenFactory(IOpenFactoryConfiguration opfc) throws OpenConfigurationException {
        if (openFactories == null) {
            openFactories = new HashMap<String, IOpenFactoryConfiguration>();
        }

        if (opfc.getName() == null) {
            throw new OpenConfigurationException("The factory must have a name", opfc.getUri(), null);
        }
        if (openFactories.containsKey(opfc.getName())) {
            throw new OpenConfigurationException("Duplicated name: " + opfc.getName(), opfc.getUri(), null);
        }

        openFactories.put(opfc.getName(), opfc);
    }

   public NodeBinderFactoryConfiguration getBinderFactory() {
        return binderFactory;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(java.lang.String,
     *      org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        IOpenCast cast = typeCastFactory == null ? null : typeCastFactory.getCast(from, to, configurationContext);
        if (cast != null) {
            return cast;
        }
        return parent == null ? null : parent.getCast(from, to);
    }

   public IConfigurableResourceContext getConfigurationContext() {
        return configurationContext;
    }

    public synchronized IGrammar getGrammar() throws OpenConfigurationException {
        return grammarFactory == null ? parent.getGrammar() : (IGrammar) grammarFactory
                .getResource(configurationContext);
    }

    public ClassFactory getGrammarFactory() {
        return grammarFactory;
    }

    public IMethodCaller getMethodCaller(String namespace, String name, IOpenClass[] params, ICastFactory casts)
            throws AmbiguousMethodException {
        IMethodCaller mc = methodFactory == null ? null : methodFactory.getMethodCaller(namespace, name, params, casts,
                configurationContext);

        if (mc != null) {
            return mc;
        }

        return parent == null ? null : parent.getMethodCaller(namespace, name, params, casts);

    }

    public LibraryFactoryConfiguration getMethodFactory() {
        return methodFactory;
    }

    public INodeBinder getNodeBinder(ISyntaxNode node) {
        INodeBinder binder = binderFactory == null ? null : binderFactory.getNodeBinder(node, configurationContext);
        if (binder != null) {
            return binder;
        }
        return parent == null ? null : parent.getNodeBinder(node);
    }

    public IOpenFactory getOpenFactory(String name) {
        OpenFactoryConfiguration conf = openFactories == null ? null : (OpenFactoryConfiguration) openFactories
                .get(name);

        if (conf != null) {
            return conf.getOpenFactory(configurationContext);
        }

        if (parent != null) {
            return parent.getOpenFactory(name);
        }

        return null;
    }

    public IOpenClass getType(String namespace, String name) {
        IOpenClass type = typeFactory == null ? null : typeFactory.getType(namespace, name, configurationContext);
        if (type != null) {
            return type;
        }
        return parent == null ? null : parent.getType(namespace, name);
    }

    public TypeCastFactory getTypeCastFactory() {
        return typeCastFactory;
    }

    public String getUri() {
        return uri;
    }

    public IOpenField getVar(String namespace, String name, boolean strictMatch) {
        IOpenField field = methodFactory == null ? null : methodFactory.getVar(namespace, name, configurationContext,
                strictMatch);
        if (field != null) {
            return field;
        }
        return parent == null ? null : parent.getVar(namespace, name, strictMatch);
    }

    public void setBinderFactory(NodeBinderFactoryConfiguration factory) {
        binderFactory = factory;
    }

    public void setConfigurationContext(IConfigurableResourceContext context) {
        configurationContext = context;
    }

    public void setGrammarFactory(ClassFactory factory) {
        grammarFactory = factory;
    }

    public void setMethodFactory(LibraryFactoryConfiguration factory) {
        methodFactory = factory;
    }

    public void setParent(IOpenLConfiguration configuration) {
        parent = configuration;
    }

    public void setTypeCastFactory(TypeCastFactory factory) {
        typeCastFactory = factory;
    }

    public void setTypeFactory(TypeFactoryConfiguration configuration) {
        typeFactory = configuration;
    }

    public void setUri(String string) {
        uri = string;
    }

    public synchronized void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        if (grammarFactory != null) {
            grammarFactory.validate(cxt);
        } else if (parent == null) {
            throw new OpenConfigurationException("Grammar class is not set", getUri(), null);
        }

        if (binderFactory != null) {
            binderFactory.validate(cxt);
        } else if (parent == null) {
            throw new OpenConfigurationException("Bindings are not set", getUri(), null);
        }

        // Methods and casts are optional
        // else if (parent == null)
        // throw new OpenConfigurationException("Methods are not set", getUri(),
        // null);

        if (methodFactory != null) {
            methodFactory.validate(cxt);
        }

        if (typeCastFactory != null) {
            typeCastFactory.validate(cxt);
        }

        if (typeFactory != null) {
            typeFactory.validate(cxt);
        }

        if (openFactories != null) {
            for (IOpenFactoryConfiguration factory : openFactories.values()) {
                factory.validate(cxt);
            }
        }

    }

}

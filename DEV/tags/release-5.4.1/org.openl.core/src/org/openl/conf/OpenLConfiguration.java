/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.OpenConfigurationException;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.ICastFactory;
import org.openl.binding.INodeBinder;
import org.openl.conf.cache.CacheUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.grammar.IGrammar;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenFactory;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class OpenLConfiguration implements IOpenLConfiguration {

    // static WeakCache configurations = new WeakCache();
    static HashMap<Object, IOpenLConfiguration> configurations = new HashMap<Object, IOpenLConfiguration>();

    // static HashMap sharedConfigurations = new HashMap();

    String uri;

    IOpenLConfiguration parent;

    IConfigurableResourceContext configurationContext;

    ClassFactory grammarFactory;

    NodeBinderFactoryConfiguration binderFactory;

    LibraryFactoryConfiguration methodFactory;
    TypeCastFactory typeCastFactory;

    TypeFactoryConfiguration typeFactory;

    Map<String, IOpenFactoryConfiguration> openFactories = null;

    static public IOpenLConfiguration getInstance(String name, IUserContext ucxt) throws OpenConfigurationException {
        IOpenLConfiguration opc = configurations.get(name);

        if (opc != null) {
            return opc;
        }

        Object key = CacheUtils.makeKey(name, ucxt);

        return configurations.get(key);

    }

    static synchronized public void register(String name, IUserContext ucxt, IOpenLConfiguration oplc, boolean shared)
            throws OpenConfigurationException {
        Object key = null;

        if (shared) {
            key = name;
        } else {
            key = CacheUtils.makeKey(name, ucxt);
        }

        IOpenLConfiguration old = configurations.get(key);
        if (old != null) {
            throw new OpenConfigurationException("The configuration " + name + " already exists", null, null);
        }
        configurations.put(key, oplc);

    }

    static public void reset() {
        configurations = new HashMap<Object, IOpenLConfiguration>();
    }

    static synchronized public void unregister(String name, IUserContext ucxt) throws OpenConfigurationException {
        Object key = CacheUtils.makeKey(name, ucxt);

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

    /**
     * @return
     */
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

    /**
     * @return
     */
    public IConfigurableResourceContext getConfigurationContext() {
        return configurationContext;
    }

    public synchronized IGrammar getGrammar() throws OpenConfigurationException {
        return grammarFactory == null ? parent.getGrammar() : (IGrammar) grammarFactory
                .getResource(configurationContext);
    }

    /**
     * @return
     */
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

    /**
     * @return
     */
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

    /**
     * @return
     */
    public TypeCastFactory getTypeCastFactory() {
        return typeCastFactory;
    }

    /**
     * @return
     */
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

    /**
     * @param factory
     */
    public void setBinderFactory(NodeBinderFactoryConfiguration factory) {
        binderFactory = factory;
    }

    /**
     * @param context
     */
    public void setConfigurationContext(IConfigurableResourceContext context) {
        configurationContext = context;
    }

    /**
     * @param factory
     */
    public void setGrammarFactory(ClassFactory factory) {
        grammarFactory = factory;
    }

    /**
     * @param factory
     */
    public void setMethodFactory(LibraryFactoryConfiguration factory) {
        methodFactory = factory;
    }

    /**
     * @param configuration
     */
    public void setParent(IOpenLConfiguration configuration) {
        parent = configuration;
    }

    /**
     * @param factory
     */
    public void setTypeCastFactory(TypeCastFactory factory) {
        typeCastFactory = factory;
    }

    /**
     * @param configuration
     */
    public void setTypeFactory(TypeFactoryConfiguration configuration) {
        typeFactory = configuration;
    }

    /**
     * @param string
     */
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
            for (Iterator<IOpenFactoryConfiguration> iter = openFactories.values().iterator(); iter.hasNext();) {
                IOpenFactoryConfiguration factory = iter.next();
                factory.validate(cxt);
            }
        }

    }

}

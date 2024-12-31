package org.openl;

import org.openl.binding.impl.Binder;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.OpenLConfigurationException;
import org.openl.conf.TypeResolver;
import org.openl.rules.lang.xls.Parser;
import org.openl.rules.vm.SimpleRulesVM;

/**
 * This class describes OpenL engine context abstraction that used during compilation process.
 * <p>
 * The class OpenL implements both factory(static) methods for creating OpenL instances and actual OpenL functionality.
 * Each instance of OpenL should be considered as a Language Configuration(LC). You may have as many LCs in your
 * application as you want. Current OpenL architecture allows to have different OpenL configurations in separate class
 * loaders, so they will not interfere with each other. It allows, for example, to have 2 LCs using different SAX or DOM
 * parser implementation.
 * <p>
 *
 * @author snshor
 */
public class OpenL {
    private IOpenParser parser;

    private IOpenBinder binder;

    private IOpenVM vm;

    public OpenL() {
    }

    /**
     * Gets instance of <code>OpenL</code> with given name.
     *
     * @return instance of OpenL
     * @throws OpenLConfigurationException
     */
    // TODO: Do not use this method! Should be removed!
    public static synchronized OpenL getInstance() {

        var librariesRegistry = new LibrariesRegistry();
        var castFactory = new CastFactory();
        castFactory.setMethodFactory(librariesRegistry.asMethodFactory());
        var methodFactory = librariesRegistry.asMethodFactory2();
        var varFactory = librariesRegistry.asVarFactory();
        var typeFactory = new TypeResolver(OpenL.class.getClassLoader());

        OpenL op = new OpenL();
        op.setParser(new Parser());
        op.setBinder(new Binder(methodFactory, castFactory, varFactory, typeFactory, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

    /**
     * Gets parser that configured for current OpenL instance.
     *
     * @return {@link IOpenParser} instance
     */
    public IOpenParser getParser() {
        return parser;
    }

    /**
     * Sets parser to current OpenL instance.
     *
     * @param parser {@link IOpenParser} instance
     */
    public void setParser(IOpenParser parser) {
        this.parser = parser;
    }

    /**
     * Gets virtual machine which used during rules execution.
     *
     * @return {@link IOpenVM} instance
     */
    public IOpenVM getVm() {
        return vm;
    }

    /**
     * Sets virtual machine.
     *
     * @param openVM {@link IOpenVM} instance
     */
    public void setVm(IOpenVM openVM) {
        vm = openVM;
    }

    /**
     * Gets binder that configured for current OpenL instance.
     *
     * @return {@link IOpenBinder} instance
     */
    public IOpenBinder getBinder() {
        return binder;
    }

    /**
     * Sets binder to current OpenL instance.
     *
     * @param binder {@link IOpenBinder} instance
     */
    public void setBinder(IOpenBinder binder) {
        this.binder = binder;
    }
}
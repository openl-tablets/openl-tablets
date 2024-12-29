package org.openl.rules.lang.xls;

import java.util.Collection;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.IUserContext;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.OpenLConfigurationException;
import org.openl.conf.OperatorsNamespace;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeResolver;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.syntax.impl.Parser;

public class OpenLBuilderImpl implements IOpenLBuilder {

    private String category;

    private String[] packageImports = new String[]{};
    private Collection<Class<?>> classImports;
    private Collection<Class<?>> libraries;

    @Override
    public OpenL build(IUserContext userContext) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader userEnvironmentContextClassLoader = userContext.getUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(userEnvironmentContextClassLoader);
            OpenL.getInstance(OpenL.OPENL_J_NAME, userContext);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        IOpenLConfiguration conf = userContext.getOpenLConfiguration(category);
        if (conf == null) {
            OpenLConfiguration oPconf = getOpenLConfiguration();
            IOpenLConfiguration extendsConfiguration = null;
            if ((extendsConfiguration = userContext.getOpenLConfiguration(OpenL.OPENL_J_NAME)) == null) {
                throw new OpenLConfigurationException(
                        "The extended category " + OpenL.OPENL_J_NAME + " must have been loaded first",
                        null);
            }

            oPconf.setParent(extendsConfiguration);
            oPconf.setClassLoader(userContext.getUserClassLoader());
            oPconf.validate();

            userContext.registerOpenLConfiguration(category, oPconf);
            conf = oPconf;
        }

        OpenL op = new OpenL();
        op.setParser(new Parser(conf));
        op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

    private OpenLConfiguration getOpenLConfiguration() {

        var op = new OpenLConfiguration();

        if (libraries != null && !libraries.isEmpty()) {
            LibrariesRegistry thisNamespaceLibrary = new LibrariesRegistry();
            LibrariesRegistry operationNamespaceLibrary = null;
            TypeCastFactory typeCastFactory = op.createTypeCastFactory();

            for (Class<?> libraryName : this.libraries) {
                if (libraryName.getAnnotation(OperatorsNamespace.class) != null) {
                    if (operationNamespaceLibrary == null) {
                        operationNamespaceLibrary = new LibrariesRegistry();
                    }
                    operationNamespaceLibrary.addJavalib(libraryName);
                }
                thisNamespaceLibrary.addJavalib(libraryName);

                typeCastFactory.addJavaCast(libraryName);

            }
            if (operationNamespaceLibrary != null) {
                op.setOperatorsFactory(operationNamespaceLibrary);
            }
            op.setMethodFactory(thisNamespaceLibrary);
        }

        op.setTypeResolver(new TypeResolver(classImports, packageImports));

        return op;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setClassImports(Collection<Class<?>> classImports) {
        this.classImports = classImports;
    }

    public void setLibraries(Collection<Class<?>> libraries) {
        this.libraries = libraries;
    }

    public void setPackageImports(String[] packageImports) {
        this.packageImports = packageImports;
    }

}

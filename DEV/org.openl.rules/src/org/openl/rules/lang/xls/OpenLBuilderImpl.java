package org.openl.rules.lang.xls;

import java.util.Collection;

import org.openl.OpenL;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.OperatorsNamespace;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeResolver;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.vm.SimpleVM;

public class OpenLBuilderImpl extends AOpenLBuilder {

    private String extendsCategory = OpenL.OPENL_J_NAME;

    private String category;

    private String[] packageImports = new String[]{};
    private Collection<Class<?>> classImports;

    private Collection<Class<?>> libraries;

    @Override
    protected SimpleVM createVM() {
        return new SimpleRulesVM();
    }

    @Override
    public OpenL build() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader userEnvironmentContextClassLoader = getUserEnvironmentContext().getUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(userEnvironmentContextClassLoader);
            OpenL.getInstance(extendsCategory, getUserEnvironmentContext());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return super.build();
    }

    @Override
    protected String getCategory() {
        return category;
    }

    @Override
    protected String getExtendsCategory() {
        return extendsCategory;
    }

    @Override
    protected OpenLConfiguration getOpenLConfiguration() {

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

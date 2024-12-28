package org.openl.rules.lang.xls;

import org.openl.OpenL;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.JavaImportTypeConfiguration;
import org.openl.conf.JavaLibraryConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NameSpacedLibraryConfiguration;
import org.openl.conf.NameSpacedTypeConfiguration;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.OperatorsNamespace;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeCastFactory.JavaCastComponent;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.vm.SimpleVM;

public class OpenLBuilderImpl extends AOpenLBuilder {

    private String extendsCategory = OpenL.OPENL_J_NAME;

    private String category;

    private String[] packageImports = new String[]{};
    private String[] classImports = new String[]{};

    private String[] libraries = new String[]{};

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

        if (libraries != null && libraries.length > 0) {
            LibraryFactoryConfiguration libraries = op.createLibraries();
            NameSpacedLibraryConfiguration thisNamespaceLibrary = new NameSpacedLibraryConfiguration();
            thisNamespaceLibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            NameSpacedLibraryConfiguration operationNamespaceLibrary = null;
            TypeCastFactory typeCastFactory = op.createTypeCastFactory();

            for (String libraryName : this.libraries) {
                JavaLibraryConfiguration javaLib = new JavaLibraryConfiguration(libraryName);

                try {
                    Class<?> libraryClass = getUserEnvironmentContext().getUserClassLoader().loadClass(libraryName);
                    if (libraryClass.getAnnotation(OperatorsNamespace.class) != null) {
                        if (operationNamespaceLibrary == null) {
                            operationNamespaceLibrary = new NameSpacedLibraryConfiguration();
                            operationNamespaceLibrary.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
                        }
                        operationNamespaceLibrary.addJavalib(javaLib);
                    }
                } catch (ReflectiveOperationException ignore) {
                }
                thisNamespaceLibrary.addJavalib(javaLib);

                JavaCastComponent javaCastComponent = typeCastFactory.new JavaCastComponent(libraryName,
                        CastFactory.class.getName());
                typeCastFactory.addJavaCast(javaCastComponent);

            }
            if (operationNamespaceLibrary != null) {
                libraries.addConfiguredLibrary(operationNamespaceLibrary);
            }
            libraries.addConfiguredLibrary(thisNamespaceLibrary);
        }

        /**
         * <libraries>
         *
         * <library namespace="org.openl.this"> <javalib classname="org.openl.rules.helpers.RulesUtils"/> </library>
         * </libraries>
         */

        if (packageImports != null && packageImports.length > 0 || classImports != null && classImports.length > 0) {
            TypeFactoryConfiguration types = op.createTypes();
            NameSpacedTypeConfiguration typeLibrary = new NameSpacedTypeConfiguration();
            typeLibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            JavaImportTypeConfiguration javaImportTypeConfiguration = new JavaImportTypeConfiguration();
            if (packageImports != null) {
                for (String packageName : packageImports) {
                    javaImportTypeConfiguration.addPackageImport(packageName);
                }
            }
            if (classImports != null) {
                for (String className : classImports) {
                    javaImportTypeConfiguration.addClassImport(className);
                }
            }

            typeLibrary.addConfiguration(javaImportTypeConfiguration);

            types.addConfiguredTypeLibrary(typeLibrary);
        }

        /*
         *
         * <types> <typelibrary namespace="org.openl.this"> <javaimport all="${org.openl.rules.java.project.imports}"/>
         * <javaimport all="org.openl.rules.helpers"/> </typelibrary> </types>
         */

        return op;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setClassImports(String[] classImports) {
        this.classImports = classImports;
    }

    public void setLibraries(String[] libraries) {
        this.libraries = libraries;
    }

    public void setPackageImports(String[] packageImports) {
        this.packageImports = packageImports;
    }

}

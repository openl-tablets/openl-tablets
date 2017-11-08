package org.openl.conf;

import java.util.Collection;
import java.util.HashSet;

import org.openl.OpenL;
import org.openl.conf.TypeCastFactory.JavaCastComponent;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilderImpl extends AOpenLBuilder {

    private String extendsCategory = OpenL.OPENL_J_NAME;

    private String category;

    private Collection<String> packageImports = new HashSet<String>();
    private Collection<String> classImports = new HashSet<String>();

    private Collection<String> libraries = new HashSet<String>();

    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader userEnvitonmentContextClassLoader = getUserEnvironmentContext().getUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(userEnvitonmentContextClassLoader);
            OpenL.getInstance(extendsCategory, getUserEnvironmentContext());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return super.build(category);
    }

    public String getCategory() {
        return category;
    }

    public String getExtendsCategory() {
        return extendsCategory;
    }

    public Collection<String> getPackageImports() {
        return packageImports;
    }

    public Collection<String> getClassImports() {
        return classImports;
    }

    public Collection<String> getLibraries() {
        return libraries;
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(extendsCategory);
        op.setCategory(category);

        if (getLibraries() != null && !getLibraries().isEmpty()) {
            LibraryFactoryConfiguration libraries = op.createLibraries();
            NameSpacedLibraryConfiguration thisNamespaceLibrary = new NameSpacedLibraryConfiguration();
            thisNamespaceLibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            NameSpacedLibraryConfiguration operationNamespaceLibrary = null;
            TypeCastFactory typeCastFactory = op.createTypecast();

            for (String libraryName : getLibraries()) {
                JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
                javalib.setClassName(libraryName);

                try {
                    Class<?> libraryClass = getUserEnvironmentContext().getUserClassLoader().loadClass(libraryName);
                    if (libraryClass.getAnnotation(OperatorsNamespace.class) != null) {
                        if (operationNamespaceLibrary == null) {
                            operationNamespaceLibrary = new NameSpacedLibraryConfiguration();
                            operationNamespaceLibrary.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
                        }
                        operationNamespaceLibrary.addJavalib(javalib);
                    }
                } catch (Exception e) {
                }
                thisNamespaceLibrary.addJavalib(javalib);

                JavaCastComponent javaCastComponent = typeCastFactory.new JavaCastComponent();
                javaCastComponent.setLibraryClassName(libraryName);
                javaCastComponent.setClassName(org.openl.binding.impl.cast.CastFactory.class.getName());
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
         * <library namespace="org.openl.this">
         * <javalib classname="org.openl.rules.helpers.RulesUtils"/> </library>
         * </libraries>
         */

        if ((getPackageImports() != null && !getPackageImports()
            .isEmpty()) || (getClassImports() != null && !getClassImports().isEmpty())) {
            TypeFactoryConfiguration types = op.createTypes();
            NameSpacedTypeConfiguration typelibrary = new NameSpacedTypeConfiguration();
            typelibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            JavaImportTypeConfiguration javaimport = new JavaImportTypeConfiguration();
            // javaimport.setAll("com.exigen.ipb.rm.uk");
            //
            // typelibrary.addJavaImport(javaimport);

            javaimport = new JavaImportTypeConfiguration();
            if (getPackageImports() != null) {
                for (String packageName : getPackageImports()) {
                    javaimport.addPackageImport(packageName);
                }
            }
            if (getClassImports() != null) {
                for (String classeName : getClassImports()) {
                    javaimport.addClassImport(classeName);
                }
            }

            typelibrary.addConfiguration(javaimport);

            types.addConfiguredTypeLibrary(typelibrary);
        }

        /*
         * 
         * <types> <typelibrary namespace="org.openl.this"> <javaimport
         * all="${org.openl.rules.java.project.imports}"/> <javaimport
         * all="org.openl.rules.helpers"/> </typelibrary> </types>
         */

        return op;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setExtendsCategory(String extendsCategory) {
        this.extendsCategory = extendsCategory;
    }

    public void setPackageImports(Collection<String> packageNames) {
        this.packageImports = packageNames;
    }

    public void setClassImports(Collection<String> classImports) {
        this.classImports = classImports;
    }
    
    public void setLibraries(Collection<String> libraries) {
        this.libraries = libraries;
    }


}

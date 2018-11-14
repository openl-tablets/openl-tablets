package org.openl.rules.java;

import org.openl.OpenL;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.JavaImportTypeConfiguration;
import org.openl.conf.JavaLibraryConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NameSpacedLibraryConfiguration;
import org.openl.conf.NameSpacedTypeConfiguration;
import org.openl.conf.NoAntOpenLTask;
import org.openl.conf.NodeBinderFactoryConfiguration;
import org.openl.conf.NodeBinderFactoryConfiguration.SingleBinderFactory;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder {

    private static final String[] JAVA_LIBRARY_NAMES = new String[] {
            org.openl.rules.util.Round.class.getName(),
            org.openl.rules.util.Booleans.class.getName(),
            org.openl.rules.util.Strings.class.getName(),
            org.openl.rules.util.Dates.class.getName(),
            org.openl.rules.util.Arrays.class.getName(),
            org.openl.rules.util.Statistics.class.getName(),
            org.openl.rules.util.Sum.class.getName(),
            org.openl.rules.util.Avg.class.getName(),
            org.openl.rules.util.Miscs.class.getName(),
            org.openl.rules.helpers.RulesUtils.class.getName(),
            org.openl.rules.dt.algorithm.evaluator.CtrUtils.class.getName(),
            org.openl.meta.ByteValue.class.getName(),
            org.openl.meta.ShortValue.class.getName(),
            org.openl.meta.IntValue.class.getName(),
            org.openl.meta.LongValue.class.getName(),
            org.openl.meta.FloatValue.class.getName(),
            org.openl.meta.DoubleValue.class.getName(),
            org.openl.meta.StringValue.class.getName(),
            org.openl.meta.ObjectValue.class.getName(),
            org.openl.meta.BigIntegerValue.class.getName(),
            org.openl.meta.BigDecimalValue.class.getName() };

    private static final String[] JAVA_OPERATORS_CLASSES = new String[] {
            org.openl.binding.impl.Operators.class.getName(),
            org.openl.binding.impl.operator.Comparison.class.getName(),
            org.openl.meta.ByteValue.class.getName(),
            org.openl.meta.ShortValue.class.getName(),
            org.openl.meta.IntValue.class.getName(),
            org.openl.meta.LongValue.class.getName(),
            org.openl.meta.FloatValue.class.getName(),
            org.openl.meta.DoubleValue.class.getName(),
            org.openl.meta.BigIntegerValue.class.getName(),
            org.openl.meta.StringValue.class.getName(),
            org.openl.meta.ObjectValue.class.getName(),
            org.openl.meta.BigDecimalValue.class.getName() };

    private static final String[] JAVA_TYPE_CAST_CLASSES = new String[] { org.openl.meta.ByteValue.class.getName(),
            org.openl.meta.ShortValue.class.getName(),
            org.openl.meta.IntValue.class.getName(),
            org.openl.meta.LongValue.class.getName(),
            org.openl.meta.FloatValue.class.getName(),
            org.openl.meta.DoubleValue.class.getName(),
            org.openl.meta.BigIntegerValue.class.getName(),
            org.openl.meta.StringValue.class.getName(),
            org.openl.meta.ObjectValue.class.getName(),
            org.openl.meta.BigDecimalValue.class.getName(),
            org.openl.rules.helpers.IntRange.class.getName(),
            org.openl.rules.helpers.DoubleRange.class.getName(),
            org.openl.rules.helpers.CharRange.class.getName()};

    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL.getInstance(OpenL.OPENL_J_NAME, getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(OpenL.OPENL_J_NAME);
        op.setCategory(OpenL.OPENL_JAVA_NAME);

        String[] binders = { "function", org.openl.binding.impl.ce.MethodNodeBinder.class.getName(), 
                             "op.ternary.qmark", org.openl.binding.impl.IfNodeBinderWithCSRSupport.class.getName()};

        NodeBinderFactoryConfiguration nbc = op.createBindings();

        for (int i = 0; i < binders.length / 2; i++) {
            SingleBinderFactory sbf = new SingleBinderFactory();
            sbf.setNode(binders[2 * i]);
            sbf.setClassName(binders[2 * i + 1]);
            nbc.addConfiguredBinder(sbf);
        }

        LibraryFactoryConfiguration libraries = op.createLibraries();

        NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
        library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        for (String javaLibConfiguration : JAVA_LIBRARY_NAMES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(javaLibConfiguration);
            library.addJavalib(javalib);
        }

        libraries.addConfiguredLibrary(library);

        NameSpacedLibraryConfiguration nslc = new NameSpacedLibraryConfiguration();
        nslc.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
        for (String className : JAVA_OPERATORS_CLASSES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(className);
            nslc.addJavalib(javalib);
        }
        libraries.addConfiguredLibrary(nslc);

        TypeCastFactory typecast = op.createTypecast();
        for (String typeCastClassName : JAVA_TYPE_CAST_CLASSES) {
            TypeCastFactory.JavaCastComponent javacast = typecast.new JavaCastComponent();
            javacast.setLibraryClassName(typeCastClassName);
            javacast.setClassName(org.openl.binding.impl.cast.CastFactory.class.getName());
            typecast.addJavaCast(javacast);
        }

        /**
         * <libraries>
         *
         * <library namespace="org.openl.this">
         * <javalib classname="org.openl.rules.helpers.RulesUtils"/> </library>
         * </libraries>
         */

        TypeFactoryConfiguration types = op.createTypes();

        NameSpacedTypeConfiguration typelibrary = new NameSpacedTypeConfiguration();
        typelibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        JavaImportTypeConfiguration javaimport1 = new JavaImportTypeConfiguration();
        javaimport1.addPackageImport("org.openl.rules.helpers");
        typelibrary.addConfiguration(javaimport1);

        JavaImportTypeConfiguration javaimport2 = new JavaImportTypeConfiguration();
        javaimport1.addPackageImport("org.openl.meta");
        typelibrary.addConfiguration(javaimport2);

        JavaImportTypeConfiguration javaimport3 = new JavaImportTypeConfiguration();
        javaimport1.addPackageImport("org.openl.rules.helpers.scope");
        typelibrary.addConfiguration(javaimport3);

        JavaImportTypeConfiguration javaimport4 = new JavaImportTypeConfiguration();
        javaimport1.addPackageImport("org.openl.rules.calc");
        javaimport1.addPackageImport("org.openl.rules.calc.result");
        typelibrary.addConfiguration(javaimport4);

        types.addConfiguredTypeLibrary(typelibrary);

        /*
         *
         * <types> <typelibrary namespace="org.openl.this"> <javaimport
         * all="${org.openl.rules.java.project.imports}"/> <javaimport
         * all="org.openl.rules.helpers"/> </typelibrary> </types>
         *
         */

        return op;
    }

}

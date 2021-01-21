package org.openl.rules.java;

import org.openl.OpenL;
import org.openl.conf.*;
import org.openl.conf.NodeBinderFactoryConfiguration.SingleBinderFactory;
import org.openl.meta.*;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.enumeration.CaProvincesEnum;
import org.openl.rules.enumeration.CaRegionsEnum;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.rules.enumeration.OriginsEnum;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.InOrNotIn;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.vm.SimpleVM;

public class OpenLBuilder extends AOpenLBuilder {

    private static final String[] JAVA_LIBRARY_NAMES = new String[] { org.openl.rules.util.Round.class.getName(),
            org.openl.rules.util.Booleans.class.getName(),
            org.openl.rules.util.Strings.class.getName(),
            org.openl.rules.util.Dates.class.getName(),
            org.openl.rules.util.Arrays.class.getName(),
            org.openl.rules.util.Statistics.class.getName(),
            org.openl.rules.util.Sum.class.getName(),
            org.openl.rules.util.Product.class.getName(),
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
            org.openl.rules.helpers.CharRange.class.getName(),
            org.openl.rules.helpers.StringRange.class.getName(),
            org.openl.rules.helpers.DateRange.class.getName() };

    @Override
    protected SimpleVM createVM() {
        return new SimpleRulesVM();
    }

    @Override
    public OpenL build(String category) {
        OpenL.getInstance(OpenL.OPENL_J_NAME, getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(OpenL.OPENL_J_NAME);
        op.setCategory(OpenL.OPENL_JAVA_NAME);

        String[] binders = { "function",
                org.openl.binding.impl.ce.MethodNodeBinder.class.getName(),
                "op.ternary.qmark",
                org.openl.binding.impl.IfNodeBinderWithCSRSupport.class.getName() };

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
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration(javaLibConfiguration);
            library.addJavalib(javalib);
        }

        libraries.addConfiguredLibrary(library);

        NameSpacedLibraryConfiguration nslc = new NameSpacedLibraryConfiguration();
        nslc.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
        for (String className : JAVA_OPERATORS_CLASSES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration(className);
            nslc.addJavalib(javalib);
        }
        libraries.addConfiguredLibrary(nslc);

        TypeCastFactory typecast = op.createTypecast();
        for (String typeCastClassName : JAVA_TYPE_CAST_CLASSES) {
            TypeCastFactory.JavaCastComponent javacast = typecast.new JavaCastComponent(typeCastClassName,
                org.openl.binding.impl.cast.CastFactory.class.getName());
            typecast.addJavaCast(javacast);
        }

        /**
         * <libraries>
         *
         * <library namespace="org.openl.this"> <javalib classname="org.openl.rules.helpers.RulesUtils"/> </library>
         * </libraries>
         */

        TypeFactoryConfiguration types = op.createTypes();

        NameSpacedTypeConfiguration typeLibrary = new NameSpacedTypeConfiguration();
        typeLibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        JavaImportTypeConfiguration javaImport1 = new JavaImportTypeConfiguration();
        javaImport1.addClassImport(CharRange.class.getName());
        javaImport1.addClassImport(DateRange.class.getName());
        javaImport1.addClassImport(IntRange.class.getName());
        javaImport1.addClassImport(StringRange.class.getName());
        javaImport1.addClassImport(DoubleRange.class.getName());
        javaImport1.addClassImport(InOrNotIn.class.getName());

        javaImport1.addClassImport(ByteValue.class.getName());
        javaImport1.addClassImport(ShortValue.class.getName());
        javaImport1.addClassImport(IntValue.class.getName());
        javaImport1.addClassImport(LongValue.class.getName());
        javaImport1.addClassImport(FloatValue.class.getName());
        javaImport1.addClassImport(DoubleValue.class.getName());
        javaImport1.addClassImport(StringValue.class.getName());
        javaImport1.addClassImport(BigIntegerValue.class.getName());
        javaImport1.addClassImport(BigDecimalValue.class.getName());
        javaImport1.addClassImport(ObjectValue.class.getName());

        javaImport1.addClassImport(SpreadsheetResult.class.getName());

        javaImport1.addClassImport(CaProvincesEnum.class.getName());
        javaImport1.addClassImport(CaRegionsEnum.class.getName());
        javaImport1.addClassImport(CountriesEnum.class.getName());
        javaImport1.addClassImport(CurrenciesEnum.class.getName());
        javaImport1.addClassImport(LanguagesEnum.class.getName());
        javaImport1.addClassImport(RegionsEnum.class.getName());
        javaImport1.addClassImport(OriginsEnum.class.getName());
        javaImport1.addClassImport(UsRegionsEnum.class.getName());
        javaImport1.addClassImport(UsStatesEnum.class.getName());

        typeLibrary.addConfiguration(javaImport1);

        types.addConfiguredTypeLibrary(typeLibrary);

        /*
         *
         * <types> <typeLibrary namespace="org.openl.this"> <javaimport all="${org.openl.rules.java.project.imports}"/>
         * <javaimport all="org.openl.rules.helpers"/> </typeLibrary> </types>
         *
         */

        return op;
    }

}

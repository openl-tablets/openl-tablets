package org.openl.j;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.Operators;
import org.openl.binding.impl.cast.CastOperators;
import org.openl.binding.impl.operator.Comparison;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.IUserContext;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeResolver;
import org.openl.rules.binding.TableProperties;
import org.openl.rules.calc.AnySpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.dt.algorithm.evaluator.CtrUtils;
import org.openl.rules.enumeration.CaProvincesEnum;
import org.openl.rules.enumeration.CaRegionsEnum;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.DTEmptyResultProcessingEnum;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.rules.enumeration.OriginsEnum;
import org.openl.rules.enumeration.RecalculateEnum;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.RulesUtils;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.lang.xls.Parser;
import org.openl.rules.util.Arrays;
import org.openl.rules.util.Avg;
import org.openl.rules.util.Booleans;
import org.openl.rules.util.Dates;
import org.openl.rules.util.Miscs;
import org.openl.rules.util.Numbers;
import org.openl.rules.util.Product;
import org.openl.rules.util.Round;
import org.openl.rules.util.Statistics;
import org.openl.rules.util.Strings;
import org.openl.rules.util.Sum;
import org.openl.rules.vm.SimpleRulesVM;

public class OpenLBuilder implements IOpenLBuilder {

    @Override
    public OpenL build(IUserContext ucxt) {
        IOpenLConfiguration conf = ucxt.getOpenLConfiguration(OpenL.OPENL_J_NAME);
        if (conf == null) {
            OpenLConfiguration oPconf = getOpenLConfiguration(ucxt.getUserClassLoader());

            ucxt.registerOpenLConfiguration(OpenL.OPENL_J_NAME, oPconf);
            conf = oPconf;
        }

        OpenL op = new OpenL();
        op.setParser(new Parser());
        op.setBinder(new Binder(conf, conf, conf, conf, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

    private static OpenLConfiguration getOpenLConfiguration(ClassLoader classLoader) {
        var op = new OpenLConfiguration();

        LibrariesRegistry library = new LibrariesRegistry();
        library.addJavalib(Round.class);
        library.addJavalib(Booleans.class);
        library.addJavalib(Strings.class);
        library.addJavalib(Dates.class);
        library.addJavalib(Arrays.class);
        library.addJavalib(Statistics.class);
        library.addJavalib(Sum.class);
        library.addJavalib(Product.class);
        library.addJavalib(Avg.class);
        library.addJavalib(Miscs.class);
        library.addJavalib(Numbers.class);
        library.addJavalib(RulesUtils.class);
        library.addJavalib(CtrUtils.class);
        op.setMethodFactory(library);

        LibrariesRegistry operators = new LibrariesRegistry();
        operators.addJavalib(Operators.class);
        operators.addJavalib(Comparison.class);
        op.setOperatorsFactory(operators);

        op.setTypeResolver(new TypeResolver(classLoader));

        TypeCastFactory typecast = op.createTypeCastFactory();
        typecast.addJavaCast(CastOperators.class);
        typecast.addJavaCast(IntRange.class);
        typecast.addJavaCast(DoubleRange.class);
        typecast.addJavaCast(CharRange.class);
        typecast.addJavaCast(StringRange.class);
        typecast.addJavaCast(DateRange.class);

        return op;

    }
}

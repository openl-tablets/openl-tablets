package org.openl.j;

import org.openl.OpenL;
import org.openl.binding.impl.ArrayInitializationBinder;
import org.openl.binding.impl.AssignOperatorNodeBinder;
import org.openl.binding.impl.BExChainBinder;
import org.openl.binding.impl.BExChainSuffixBinder;
import org.openl.binding.impl.BinaryOperatorAndNodeBinder;
import org.openl.binding.impl.BinaryOperatorNodeBinder;
import org.openl.binding.impl.BinaryOperatorOrNodeBinder;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.BlockBinder;
import org.openl.binding.impl.BusinessIntNodeBinder;
import org.openl.binding.impl.CharNodeBinder;
import org.openl.binding.impl.DoubleNodeBinder;
import org.openl.binding.impl.ForNodeBinder;
import org.openl.binding.impl.IdentifierBinder;
import org.openl.binding.impl.IdentifierSequenceBinder;
import org.openl.binding.impl.IfNodeBinder;
import org.openl.binding.impl.IfNodeBinderWithCSRSupport;
import org.openl.binding.impl.IndexNodeBinder;
import org.openl.binding.impl.IndexParameterDeclarationBinder;
import org.openl.binding.impl.IntNodeBinder;
import org.openl.binding.impl.ListNodeBinder;
import org.openl.binding.impl.LiteralNodeBinder;
import org.openl.binding.impl.LocalVarBinder;
import org.openl.binding.impl.MethodHeaderNodeBinder;
import org.openl.binding.impl.NewArrayNodeBinder;
import org.openl.binding.impl.NewNodeBinder;
import org.openl.binding.impl.Operators;
import org.openl.binding.impl.OrderByIndexNodeBinder;
import org.openl.binding.impl.ParameterDeclarationNodeBinder;
import org.openl.binding.impl.PercentNodeBinder;
import org.openl.binding.impl.PrefixOperatorNodeBinder;
import org.openl.binding.impl.RangeVariableBinder;
import org.openl.binding.impl.ReturnNodeBinder;
import org.openl.binding.impl.SelectAllIndexNodeBinder;
import org.openl.binding.impl.SelectFirstIndexNodeBinder;
import org.openl.binding.impl.SplitByIndexNodeBinder;
import org.openl.binding.impl.StringNodeBinder;
import org.openl.binding.impl.SuffixOperatorNodeBinder;
import org.openl.binding.impl.TransformIndexNodeBinder;
import org.openl.binding.impl.TypeBinder;
import org.openl.binding.impl.TypeCastBinder;
import org.openl.binding.impl.UnaryOperatorNodeBinder;
import org.openl.binding.impl.WhereExpressionNodeBinder;
import org.openl.binding.impl.WhereVarNodeBinder;
import org.openl.binding.impl.WhileNodeBinder;
import org.openl.binding.impl.cast.CastOperators;
import org.openl.binding.impl.ce.MethodNodeBinder;
import org.openl.binding.impl.module.MethodDeclarationNodeBinder;
import org.openl.binding.impl.module.MethodParametersNodeBinder;
import org.openl.binding.impl.module.ParameterDeclarationNodeBinderWithContextParameterSupport;
import org.openl.binding.impl.module.VarDeclarationNodeBinder;
import org.openl.binding.impl.operator.Comparison;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.IUserContext;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.NodeBinders;
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
import org.openl.syntax.impl.Parser;

public class OpenLBuilder implements IOpenLBuilder {

    static {
        // OpenL types which are located in 'rules' module, but must be registered in the core by default.
        TypeResolver.putClass(CharRange.class);
        TypeResolver.putClass(DateRange.class);
        TypeResolver.putClass(IntRange.class);
        TypeResolver.putClass(StringRange.class);
        TypeResolver.putClass(DoubleRange.class);

        TypeResolver.putClass(SpreadsheetResult.class);
        TypeResolver.putClass(AnySpreadsheetResult.class);
        TypeResolver.putClass(TableProperties.class);

        TypeResolver.putClass(CaProvincesEnum.class);
        TypeResolver.putClass(CaRegionsEnum.class);
        TypeResolver.putClass(CountriesEnum.class);
        TypeResolver.putClass(CurrenciesEnum.class);
        TypeResolver.putClass(LanguagesEnum.class);
        TypeResolver.putClass(RegionsEnum.class);
        TypeResolver.putClass(OriginsEnum.class);
        TypeResolver.putClass(UsRegionsEnum.class);
        TypeResolver.putClass(UsStatesEnum.class);
        TypeResolver.putClass(DTEmptyResultProcessingEnum.class);
        TypeResolver.putClass(RecalculateEnum.class);
        TypeResolver.putClass(ValidateDTEnum.class);
    }

    @Override
    public OpenL build(IUserContext ucxt) {
        IOpenLConfiguration conf = ucxt.getOpenLConfiguration(OpenL.OPENL_J_NAME);
        if (conf == null) {
            OpenLConfiguration oPconf = getOpenLConfiguration(ucxt.getUserClassLoader());
            oPconf.validate();

            ucxt.registerOpenLConfiguration(OpenL.OPENL_J_NAME, oPconf);
            conf = oPconf;
        }

        OpenL op = new OpenL();
        op.setParser(new Parser(conf));
        op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

    private static OpenLConfiguration getOpenLConfiguration(ClassLoader classLoader) {
        var op = new OpenLConfiguration();

        op.setGrammar(BExGrammarWithParsingHelp::new);

        op.setNodeBinders(createNodeBinders());

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

    private static NodeBinders createNodeBinders() {
        NodeBinders binders = new NodeBinders();

        binders.put("literal", new LiteralNodeBinder());
        binders.put("literal.integer", new IntNodeBinder());
        binders.put("literal.real", new DoubleNodeBinder());
        binders.put("literal.percent", new PercentNodeBinder());
        binders.put("literal.string", new StringNodeBinder());
        binders.put("literal.char", new CharNodeBinder());
        binders.put("literal.integer.business", new BusinessIntNodeBinder());
        binders.put("array.init", new ArrayInitializationBinder());
        binders.put("method.header", new MethodHeaderNodeBinder());
        binders.put("param.declaration", new ParameterDeclarationNodeBinder());
        binders.put("method.parameters", new MethodParametersNodeBinder());
        binders.put("method.declaration", new MethodDeclarationNodeBinder());
        binders.put("var.declaration", new VarDeclarationNodeBinder());
        binders.put("parameter.declaration", new ParameterDeclarationNodeBinderWithContextParameterSupport());
        binders.put("block", new BlockBinder());
        binders.put("op.binary", new BinaryOperatorNodeBinder());
        binders.put("op.binary.and", new BinaryOperatorAndNodeBinder());
        binders.put("op.binary.or", new BinaryOperatorOrNodeBinder());
        binders.put("op.unary", new UnaryOperatorNodeBinder());
        binders.put("op.prefix", new PrefixOperatorNodeBinder());
        binders.put("op.suffix", new SuffixOperatorNodeBinder());
        binders.put("op.assign", new AssignOperatorNodeBinder());
        binders.put("op.new.object", new NewNodeBinder());
        binders.put("op.new.array", new NewArrayNodeBinder());
        binders.put("op.index", new IndexNodeBinder());
        binders.put("selectfirst.index", new SelectFirstIndexNodeBinder());
        binders.put("selectall.index", new SelectAllIndexNodeBinder());
        binders.put("orderby.index", new OrderByIndexNodeBinder());
        binders.put("orderdecreasingby.index", new OrderByIndexNodeBinder());
        binders.put("splitby.index", new SplitByIndexNodeBinder());

        binders.put("transform.index", new TransformIndexNodeBinder());
        binders.put("transformunique.index", new TransformIndexNodeBinder());
        binders.put("index.parameter.declaration", new IndexParameterDeclarationBinder());

        binders.put("op.ternary.qmark", new IfNodeBinderWithCSRSupport());
        binders.put("type.cast", new TypeCastBinder());
        binders.put("local.var.declaration", new LocalVarBinder());
        binders.put("type.declaration", new TypeBinder());
        binders.put("function", new MethodNodeBinder());
        binders.put("identifier", new IdentifierBinder());
        binders.put("identifier.sequence", new IdentifierSequenceBinder());
        binders.put("range.variable", new RangeVariableBinder());
        binders.put("chain", new BExChainBinder());
        binders.put("chain.suffix", new BExChainSuffixBinder());
        binders.put("where.expression", new WhereExpressionNodeBinder());
        binders.put("where.var.explanation", new WhereVarNodeBinder());
        binders.put("list", new ListNodeBinder());
        binders.put("control.for", new ForNodeBinder());
        binders.put("control.if", new IfNodeBinder());
        binders.put("control.while", new WhileNodeBinder());
        binders.put("control.return", new ReturnNodeBinder());
        return binders;
    }

}

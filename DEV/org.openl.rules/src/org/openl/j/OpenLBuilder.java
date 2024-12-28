package org.openl.j;

import org.openl.OpenL;
import org.openl.binding.impl.ArrayInitializationBinder;
import org.openl.binding.impl.AssignOperatorNodeBinder;
import org.openl.binding.impl.BExChainBinder;
import org.openl.binding.impl.BExChainSuffixBinder;
import org.openl.binding.impl.BinaryOperatorAndNodeBinder;
import org.openl.binding.impl.BinaryOperatorNodeBinder;
import org.openl.binding.impl.BinaryOperatorOrNodeBinder;
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
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.CastOperators;
import org.openl.binding.impl.ce.MethodNodeBinder;
import org.openl.binding.impl.module.MethodDeclarationNodeBinder;
import org.openl.binding.impl.module.MethodParametersNodeBinder;
import org.openl.binding.impl.module.ParameterDeclarationNodeBinderWithContextParameterSupport;
import org.openl.binding.impl.module.VarDeclarationNodeBinder;
import org.openl.binding.impl.operator.Comparison;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.ClassFactory;
import org.openl.conf.JavaImportTypeConfiguration;
import org.openl.conf.JavaLibraryConfiguration;
import org.openl.conf.JavaLongNameTypeConfiguration;
import org.openl.conf.JavaTypeConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NameSpacedLibraryConfiguration;
import org.openl.conf.NameSpacedTypeConfiguration;
import org.openl.conf.NodeBinderFactoryConfiguration;
import org.openl.conf.NodeBinderFactoryConfiguration.SingleBinderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeFactoryConfiguration;
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
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.java.JavaPrimitiveTypeLibrary;
import org.openl.vm.SimpleVM;

public class OpenLBuilder extends AOpenLBuilder {

    private static final String[] JAVA_LIBRARY_NAMES = new String[]{Round.class.getName(),
            Booleans.class.getName(),
            Strings.class.getName(),
            Dates.class.getName(),
            Arrays.class.getName(),
            Statistics.class.getName(),
            Sum.class.getName(),
            Product.class.getName(),
            Avg.class.getName(),
            Miscs.class.getName(),
            Numbers.class.getName(),
            RulesUtils.class.getName(),
            CtrUtils.class.getName()};

    private static final String[] JAVA_OPERATORS_CLASSES = new String[]{
            Operators.class.getName(),
            Comparison.class.getName()};

    private static final String[] JAVA_TYPE_CAST_CLASSES = new String[]{
            CastOperators.class.getName(),
            IntRange.class.getName(),
            DoubleRange.class.getName(),
            CharRange.class.getName(),
            StringRange.class.getName(),
            DateRange.class.getName()};

    @Override
    protected SimpleVM createVM() {
        return new SimpleRulesVM();
    }

    @Override
    protected OpenLConfiguration getOpenLConfiguration() {
        var op = new OpenLConfiguration();

        ClassFactory cfg = op.createGrammar();
        cfg.setClassName(BExGrammarWithParsingHelp.class.getName());

        NodeBinderFactoryConfiguration nbc = op.createBindings();

        String[] binders = {"literal",
                LiteralNodeBinder.class.getName(),
                "literal.integer",
                IntNodeBinder.class.getName(),
                "literal.real",
                DoubleNodeBinder.class.getName(),
                "literal.percent",
                PercentNodeBinder.class.getName(),
                "literal.string",
                StringNodeBinder.class.getName(),
                "literal.char",
                CharNodeBinder.class.getName(),
                "literal.integer.business",
                BusinessIntNodeBinder.class.getName(),
                "array.init",
                ArrayInitializationBinder.class.getName(),
                "method.header",
                MethodHeaderNodeBinder.class.getName(),
                "param.declaration",
                ParameterDeclarationNodeBinder.class.getName(),
                "method.parameters",
                MethodParametersNodeBinder.class.getName(),
                "method.declaration",
                MethodDeclarationNodeBinder.class.getName(),
                "var.declaration",
                VarDeclarationNodeBinder.class.getName(),
                "parameter.declaration",
                ParameterDeclarationNodeBinderWithContextParameterSupport.class.getName(),
                "block",
                BlockBinder.class.getName(),
                "op.binary",
                BinaryOperatorNodeBinder.class.getName(),
                "op.binary.and",
                BinaryOperatorAndNodeBinder.class.getName(),
                "op.binary.or",
                BinaryOperatorOrNodeBinder.class.getName(),
                "op.unary",
                UnaryOperatorNodeBinder.class.getName(),
                "op.prefix",
                PrefixOperatorNodeBinder.class.getName(),
                "op.suffix",
                SuffixOperatorNodeBinder.class.getName(),
                "op.assign",
                AssignOperatorNodeBinder.class.getName(),
                "op.new.object",
                NewNodeBinder.class.getName(),
                "op.new.array",
                NewArrayNodeBinder.class.getName(),
                "op.index",
                IndexNodeBinder.class.getName(),
                "selectfirst.index",
                SelectFirstIndexNodeBinder.class.getName(),
                "selectall.index",
                SelectAllIndexNodeBinder.class.getName(),
                "orderby.index",
                OrderByIndexNodeBinder.class.getName(),
                "orderdecreasingby.index",
                OrderByIndexNodeBinder.class.getName(),
                "splitby.index",
                SplitByIndexNodeBinder.class.getName(),

                "transform.index",
                TransformIndexNodeBinder.class.getName(),
                "transformunique.index",
                TransformIndexNodeBinder.class.getName(),
                "index.parameter.declaration",
                IndexParameterDeclarationBinder.class.getName(),

                "op.ternary.qmark",
                IfNodeBinderWithCSRSupport.class.getName(),
                "type.cast",
                TypeCastBinder.class.getName(),
                "local.var.declaration",
                LocalVarBinder.class.getName(),
                "type.declaration",
                TypeBinder.class.getName(),
                "function",
                MethodNodeBinder.class.getName(),
                "identifier",
                IdentifierBinder.class.getName(),
                "identifier.sequence",
                IdentifierSequenceBinder.class.getName(),
                "range.variable",
                RangeVariableBinder.class.getName(),
                "chain",
                BExChainBinder.class.getName(),
                "chain.suffix",
                BExChainSuffixBinder.class.getName(),
                "where.expression",
                WhereExpressionNodeBinder.class.getName(),
                "where.var.explanation",
                WhereVarNodeBinder.class.getName(),
                "list",
                ListNodeBinder.class.getName(),
                "control.for",
                ForNodeBinder.class.getName(),
                "control.if",
                IfNodeBinder.class.getName(),
                "control.while",
                WhileNodeBinder.class.getName(),
                "control.return",
                ReturnNodeBinder.class.getName()};

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

        /*
         * <libraries> <library namespace="org.openl.operators"> <javalib classname="org.openl.binding.impl.Operators"/>
         * </library> </libraries>
         */

        TypeFactoryConfiguration types = op.createTypes();
        NameSpacedTypeConfiguration nstc = new NameSpacedTypeConfiguration();
        nstc.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        JavaTypeConfiguration javatype = new JavaTypeConfiguration();
        javatype.setClassName(JavaPrimitiveTypeLibrary.class.getName());
        nstc.addConfiguration(javatype);

        JavaImportTypeConfiguration javaImports = new JavaImportTypeConfiguration();
        // java.lang
        javaImports.addClassImport("java.lang.AbstractMethodError");
        javaImports.addClassImport("java.lang.Appendable");
        javaImports.addClassImport("java.lang.ArithmeticException");
        javaImports.addClassImport("java.lang.ArrayIndexOutOfBoundsException");
        javaImports.addClassImport("java.lang.ArrayStoreException");
        javaImports.addClassImport("java.lang.AssertionError");
        javaImports.addClassImport("java.lang.AutoCloseable");
        javaImports.addClassImport("java.lang.Boolean");
        javaImports.addClassImport("java.lang.BootstrapMethodError");
        javaImports.addClassImport("java.lang.Byte");
        javaImports.addClassImport("java.lang.Character");
        javaImports.addClassImport("java.lang.CharSequence");
        javaImports.addClassImport("java.lang.Class");
        javaImports.addClassImport("java.lang.ClassCastException");
        javaImports.addClassImport("java.lang.ClassCircularityError");
        javaImports.addClassImport("java.lang.ClassFormatError");
        javaImports.addClassImport("java.lang.ClassLoader");
        javaImports.addClassImport("java.lang.ClassNotFoundException");
        javaImports.addClassImport("java.lang.ClassValue");
        javaImports.addClassImport("java.lang.Cloneable");
        javaImports.addClassImport("java.lang.CloneNotSupportedException");
        javaImports.addClassImport("java.lang.Comparable");
        javaImports.addClassImport("java.lang.Compiler");
        javaImports.addClassImport("java.lang.Deprecated");
        javaImports.addClassImport("java.lang.Double");
        javaImports.addClassImport("java.lang.Enum");
        javaImports.addClassImport("java.lang.EnumConstantNotPresentException");
        javaImports.addClassImport("java.lang.Error");
        javaImports.addClassImport("java.lang.Exception");
        javaImports.addClassImport("java.lang.ExceptionInInitializerError");
        javaImports.addClassImport("java.lang.Float");
        javaImports.addClassImport("java.lang.FunctionalInterface");
        javaImports.addClassImport("java.lang.IllegalAccessError");
        javaImports.addClassImport("java.lang.IllegalAccessException");
        javaImports.addClassImport("java.lang.IllegalArgumentException");
        javaImports.addClassImport("java.lang.IllegalMonitorStateException");
        javaImports.addClassImport("java.lang.IllegalStateException");
        javaImports.addClassImport("java.lang.IllegalThreadStateException");
        javaImports.addClassImport("java.lang.IncompatibleClassChangeError");
        javaImports.addClassImport("java.lang.IndexOutOfBoundsException");
        javaImports.addClassImport("java.lang.InheritableThreadLocal");
        javaImports.addClassImport("java.lang.InstantiationError");
        javaImports.addClassImport("java.lang.InstantiationException");
        javaImports.addClassImport("java.lang.Integer");
        javaImports.addClassImport("java.lang.InternalError");
        javaImports.addClassImport("java.lang.InterruptedException");
        javaImports.addClassImport("java.lang.Iterable");
        javaImports.addClassImport("java.lang.LinkageError");
        javaImports.addClassImport("java.lang.Long");
        javaImports.addClassImport("java.lang.Math");
        javaImports.addClassImport("java.lang.NegativeArraySizeException");
        javaImports.addClassImport("java.lang.NoClassDefFoundError");
        javaImports.addClassImport("java.lang.NoSuchFieldError");
        javaImports.addClassImport("java.lang.NoSuchFieldException");
        javaImports.addClassImport("java.lang.NoSuchMethodError");
        javaImports.addClassImport("java.lang.NoSuchMethodException");
        javaImports.addClassImport("java.lang.NullPointerException");
        javaImports.addClassImport("java.lang.Number");
        javaImports.addClassImport("java.lang.NumberFormatException");
        javaImports.addClassImport("java.lang.Object");
        javaImports.addClassImport("java.lang.OutOfMemoryError");
        javaImports.addClassImport("java.lang.Override");
        javaImports.addClassImport("java.lang.Package");
        javaImports.addClassImport("java.lang.Process");
        javaImports.addClassImport("java.lang.ProcessBuilder");
        javaImports.addClassImport("java.lang.Readable");
        javaImports.addClassImport("java.lang.ReflectiveOperationException");
        javaImports.addClassImport("java.lang.Runnable");
        javaImports.addClassImport("java.lang.Runtime");
        javaImports.addClassImport("java.lang.RuntimeException");
        javaImports.addClassImport("java.lang.RuntimePermission");
        javaImports.addClassImport("java.lang.SafeVarargs");
        javaImports.addClassImport("java.lang.SecurityException");
        javaImports.addClassImport("java.lang.SecurityManager");
        javaImports.addClassImport("java.lang.Short");
        javaImports.addClassImport("java.lang.StackOverflowError");
        javaImports.addClassImport("java.lang.StackTraceElement");
        javaImports.addClassImport("java.lang.StrictMath");
        javaImports.addClassImport("java.lang.String");
        javaImports.addClassImport("java.lang.StringBuffer");
        javaImports.addClassImport("java.lang.StringBuilder");
        javaImports.addClassImport("java.lang.StringIndexOutOfBoundsException");
        javaImports.addClassImport("java.lang.SuppressWarnings");
        javaImports.addClassImport("java.lang.System");
        javaImports.addClassImport("java.lang.Thread");
        javaImports.addClassImport("java.lang.ThreadDeath");
        javaImports.addClassImport("java.lang.ThreadGroup");
        javaImports.addClassImport("java.lang.ThreadLocal");
        javaImports.addClassImport("java.lang.Throwable");
        javaImports.addClassImport("java.lang.TypeNotPresentException");
        javaImports.addClassImport("java.lang.UnknownError");
        javaImports.addClassImport("java.lang.UnsatisfiedLinkError");
        javaImports.addClassImport("java.lang.UnsupportedClassVersionError");
        javaImports.addClassImport("java.lang.UnsupportedOperationException");
        javaImports.addClassImport("java.lang.VerifyError");
        javaImports.addClassImport("java.lang.VirtualMachineError");
        javaImports.addClassImport("java.lang.Void");

        // java.util
        javaImports.addClassImport("java.util.AbstractCollection");
        javaImports.addClassImport("java.util.AbstractList");
        javaImports.addClassImport("java.util.AbstractMap");
        javaImports.addClassImport("java.util.AbstractQueue");
        javaImports.addClassImport("java.util.AbstractSequentialList");
        javaImports.addClassImport("java.util.AbstractSet");
        javaImports.addClassImport("java.util.ArrayDeque");
        javaImports.addClassImport("java.util.ArrayList");
        javaImports.addClassImport("java.util.Arrays");
        javaImports.addClassImport("java.util.Base64");
        javaImports.addClassImport("java.util.BitSet");
        javaImports.addClassImport("java.util.Calendar");
        javaImports.addClassImport("java.util.Collection");
        javaImports.addClassImport("java.util.Collections");
        javaImports.addClassImport("java.util.Comparator");
        javaImports.addClassImport("java.util.ConcurrentModificationException");
        javaImports.addClassImport("java.util.Currency");
        javaImports.addClassImport("java.util.Date");
        javaImports.addClassImport("java.util.Deque");
        javaImports.addClassImport("java.util.Dictionary");
        javaImports.addClassImport("java.util.DoubleSummaryStatistics");
        javaImports.addClassImport("java.util.DuplicateFormatFlagsException");
        javaImports.addClassImport("java.util.EmptyStackException");
        javaImports.addClassImport("java.util.Enumeration");
        javaImports.addClassImport("java.util.EnumMap");
        javaImports.addClassImport("java.util.EnumSet");
        javaImports.addClassImport("java.util.EventListener");
        javaImports.addClassImport("java.util.EventListenerProxy");
        javaImports.addClassImport("java.util.EventObject");
        javaImports.addClassImport("java.util.FormatFlagsConversionMismatchException");
        javaImports.addClassImport("java.util.Formattable");
        javaImports.addClassImport("java.util.FormattableFlags");
        javaImports.addClassImport("java.util.Formatter");
        javaImports.addClassImport("java.util.FormatterClosedException");
        javaImports.addClassImport("java.util.GregorianCalendar");
        javaImports.addClassImport("java.util.HashMap");
        javaImports.addClassImport("java.util.HashSet");
        javaImports.addClassImport("java.util.Hashtable");
        javaImports.addClassImport("java.util.IdentityHashMap");
        javaImports.addClassImport("java.util.IllegalFormatCodePointException");
        javaImports.addClassImport("java.util.IllegalFormatConversionException");
        javaImports.addClassImport("java.util.IllegalFormatException");
        javaImports.addClassImport("java.util.IllegalFormatFlagsException");
        javaImports.addClassImport("java.util.IllegalFormatPrecisionException");
        javaImports.addClassImport("java.util.IllegalFormatWidthException");
        javaImports.addClassImport("java.util.IllformedLocaleException");
        javaImports.addClassImport("java.util.InputMismatchException");
        javaImports.addClassImport("java.util.IntSummaryStatistics");
        javaImports.addClassImport("java.util.InvalidPropertiesFormatException");
        javaImports.addClassImport("java.util.Iterator");
        javaImports.addClassImport("java.util.LinkedHashMap");
        javaImports.addClassImport("java.util.LinkedHashSet");
        javaImports.addClassImport("java.util.LinkedList");
        javaImports.addClassImport("java.util.List");
        javaImports.addClassImport("java.util.ListIterator");
        javaImports.addClassImport("java.util.ListResourceBundle");
        javaImports.addClassImport("java.util.Locale");
        javaImports.addClassImport("java.util.LongSummaryStatistics");
        javaImports.addClassImport("java.util.Map");
        javaImports.addClassImport("java.util.MissingFormatArgumentException");
        javaImports.addClassImport("java.util.MissingFormatWidthException");
        javaImports.addClassImport("java.util.MissingResourceException");
        javaImports.addClassImport("java.util.NavigableMap");
        javaImports.addClassImport("java.util.NavigableSet");
        javaImports.addClassImport("java.util.NoSuchElementException");
        javaImports.addClassImport("java.util.Objects");
        javaImports.addClassImport("java.util.Observable");
        javaImports.addClassImport("java.util.Observer");
        javaImports.addClassImport("java.util.Optional");
        javaImports.addClassImport("java.util.OptionalDouble");
        javaImports.addClassImport("java.util.OptionalInt");
        javaImports.addClassImport("java.util.OptionalLong");
        javaImports.addClassImport("java.util.PrimitiveIterator");
        javaImports.addClassImport("java.util.PriorityQueue");
        javaImports.addClassImport("java.util.Properties");
        javaImports.addClassImport("java.util.PropertyPermission");
        javaImports.addClassImport("java.util.PropertyResourceBundle");
        javaImports.addClassImport("java.util.Queue");
        javaImports.addClassImport("java.util.Random");
        javaImports.addClassImport("java.util.RandomAccess");
        javaImports.addClassImport("java.util.ResourceBundle");
        javaImports.addClassImport("java.util.Scanner");
        javaImports.addClassImport("java.util.ServiceConfigurationError");
        javaImports.addClassImport("java.util.ServiceLoader");
        javaImports.addClassImport("java.util.Set");
        javaImports.addClassImport("java.util.SimpleTimeZone");
        javaImports.addClassImport("java.util.SortedMap");
        javaImports.addClassImport("java.util.SortedSet");
        javaImports.addClassImport("java.util.Spliterator");
        javaImports.addClassImport("java.util.Spliterators");
        javaImports.addClassImport("java.util.SplittableRandom");
        javaImports.addClassImport("java.util.Stack");
        javaImports.addClassImport("java.util.StringJoiner");
        javaImports.addClassImport("java.util.StringTokenizer");
        javaImports.addClassImport("java.util.Timer");
        javaImports.addClassImport("java.util.TimerTask");
        javaImports.addClassImport("java.util.TimeZone");
        javaImports.addClassImport("java.util.TooManyListenersException");
        javaImports.addClassImport("java.util.TreeMap");
        javaImports.addClassImport("java.util.TreeSet");
        javaImports.addClassImport("java.util.UnknownFormatConversionException");
        javaImports.addClassImport("java.util.UnknownFormatFlagsException");
        javaImports.addClassImport("java.util.UUID");
        javaImports.addClassImport("java.util.Vector");
        javaImports.addClassImport("java.util.WeakHashMap");

        //java.math
        javaImports.addClassImport("java.math.BigDecimal");
        javaImports.addClassImport("java.math.BigInteger");
        javaImports.addClassImport("java.math.MathContext");
        javaImports.addClassImport("java.math.RoundingMode");

        nstc.addConfiguration(javaImports);

        JavaImportTypeConfiguration openlTypes = new JavaImportTypeConfiguration();
        openlTypes.addClassImport(CharRange.class.getName());
        openlTypes.addClassImport(DateRange.class.getName());
        openlTypes.addClassImport(IntRange.class.getName());
        openlTypes.addClassImport(StringRange.class.getName());
        openlTypes.addClassImport(DoubleRange.class.getName());

        openlTypes.addClassImport(SpreadsheetResult.class.getName());
        openlTypes.addClassImport(AnySpreadsheetResult.class.getName());
        openlTypes.addClassImport(TableProperties.class.getName());

        openlTypes.addClassImport(CaProvincesEnum.class.getName());
        openlTypes.addClassImport(CaRegionsEnum.class.getName());
        openlTypes.addClassImport(CountriesEnum.class.getName());
        openlTypes.addClassImport(CurrenciesEnum.class.getName());
        openlTypes.addClassImport(LanguagesEnum.class.getName());
        openlTypes.addClassImport(RegionsEnum.class.getName());
        openlTypes.addClassImport(OriginsEnum.class.getName());
        openlTypes.addClassImport(UsRegionsEnum.class.getName());
        openlTypes.addClassImport(UsStatesEnum.class.getName());
        openlTypes.addClassImport(DTEmptyResultProcessingEnum.class.getName());
        openlTypes.addClassImport(RecalculateEnum.class.getName());
        openlTypes.addClassImport(ValidateDTEnum.class.getName());

        nstc.addConfiguration(openlTypes);

        JavaLongNameTypeConfiguration javaLongNameType = new JavaLongNameTypeConfiguration();
        nstc.addConfiguration(javaLongNameType);

        types.addConfiguredTypeLibrary(nstc);

        TypeCastFactory typecast = op.createTypeCastFactory();
        for (String typeCastClassName : JAVA_TYPE_CAST_CLASSES) {
            TypeCastFactory.JavaCastComponent javacast = typecast.new JavaCastComponent(typeCastClassName,
                    CastFactory.class.getName());
            typecast.addJavaCast(javacast);
        }

        return op;

    }

    @Override
    protected String getCategory() {
        return OpenL.OPENL_J_NAME;
    }

    @Override
    protected String getExtendsCategory() {
        return null;
    }
}

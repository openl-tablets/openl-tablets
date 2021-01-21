package org.openl.j;

import org.openl.OpenL;
import org.openl.conf.*;
import org.openl.conf.NodeBinderFactoryConfiguration.SingleBinderFactory;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder {

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setCategory(OpenL.OPENL_J_NAME);

        ClassFactory cfg = op.createGrammar();
        cfg.setClassName(BExGrammarWithParsingHelp.class.getName());

        NodeBinderFactoryConfiguration nbc = op.createBindings();

        String[] binders = { "literal",
                org.openl.binding.impl.LiteralNodeBinder.class.getName(),
                "literal.integer",
                org.openl.binding.impl.IntNodeBinder.class.getName(),
                "literal.real",
                org.openl.binding.impl.DoubleNodeBinder.class.getName(),
                "literal.percent",
                org.openl.binding.impl.PercentNodeBinder.class.getName(),
                "literal.string",
                org.openl.binding.impl.StringNodeBinder.class.getName(),
                "literal.char",
                org.openl.binding.impl.CharNodeBinder.class.getName(),
                "literal.integer.business",
                org.openl.binding.impl.BusinessIntNodeBinder.class.getName(),
                "array.init",
                org.openl.binding.impl.ArrayInitializationBinder.class.getName(),
                "method.header",
                org.openl.binding.impl.MethodHeaderNodeBinder.class.getName(),
                "method.parameters",
                org.openl.binding.impl.module.MethodParametersNodeBinder.class.getName(),
                "method.declaration",
                org.openl.binding.impl.module.MethodDeclarationNodeBinder.class.getName(),
                "var.declaration",
                org.openl.binding.impl.module.VarDeclarationNodeBinder.class.getName(),
                "parameter.declaration",
                org.openl.binding.impl.module.ParameterDeclarationNodeBinder.class.getName(),
                "block",
                org.openl.binding.impl.BlockBinder.class.getName(),
                "op.binary",
                org.openl.binding.impl.BinaryOperatorNodeBinder.class.getName(),
                "op.binary.and",
                org.openl.binding.impl.BinaryOperatorAndNodeBinder.class.getName(),
                "op.binary.or",
                org.openl.binding.impl.BinaryOperatorOrNodeBinder.class.getName(),
                "op.unary",
                org.openl.binding.impl.UnaryOperatorNodeBinder.class.getName(),
                "op.prefix",
                org.openl.binding.impl.PrefixOperatorNodeBinder.class.getName(),
                "op.suffix",
                org.openl.binding.impl.SuffixOperatorNodeBinder.class.getName(),
                "op.assign",
                org.openl.binding.impl.AssignOperatorNodeBinder.class.getName(),
                "op.new.object",
                org.openl.binding.impl.NewNodeBinder.class.getName(),
                "op.new.array",
                org.openl.binding.impl.NewArrayNodeBinder.class.getName(),
                "op.index",
                org.openl.binding.impl.IndexNodeBinder.class.getName(),
                "selectfirst.index",
                org.openl.binding.impl.SelectFirstIndexNodeBinder.class.getName(),
                "selectall.index",
                org.openl.binding.impl.SelectAllIndexNodeBinder.class.getName(),
                "orderby.index",
                org.openl.binding.impl.OrderByIndexNodeBinder.class.getName(),
                "orderdecreasingby.index",
                org.openl.binding.impl.OrderByIndexNodeBinder.class.getName(),
                "splitby.index",
                org.openl.binding.impl.SplitByIndexNodeBinder.class.getName(),

                "transform.index",
                org.openl.binding.impl.TransformIndexNodeBinder.class.getName(),
                "transformunique.index",
                org.openl.binding.impl.TransformIndexNodeBinder.class.getName(),
                "index.parameter.declaration",
                org.openl.binding.impl.IndexParameterDeclarationBinder.class.getName(),

                "op.ternary.qmark",
                org.openl.binding.impl.IfNodeBinder.class.getName(),
                "type.cast",
                org.openl.binding.impl.TypeCastBinder.class.getName(),
                "local.var.declaration",
                org.openl.binding.impl.LocalVarBinder.class.getName(),
                "type.declaration",
                org.openl.binding.impl.TypeBinder.class.getName(),
                "function",
                org.openl.binding.impl.MethodNodeBinder.class.getName(),
                "identifier",
                org.openl.binding.impl.IdentifierBinder.class.getName(),
                "identifier.sequence",
                org.openl.binding.impl.IdentifierSequenceBinder.class.getName(),
                "range.variable",
                org.openl.binding.impl.RangeVariableBinder.class.getName(),
                "chain",
                org.openl.binding.impl.BExChainBinder.class.getName(),
                "chain.suffix",
                org.openl.binding.impl.BExChainSuffixBinder.class.getName(),
                "where.expression",
                org.openl.binding.impl.WhereExpressionNodeBinder.class.getName(),
                "where.var.explanation",
                org.openl.binding.impl.WhereVarNodeBinder.class.getName(),
                "list",
                org.openl.binding.impl.ListNodeBinder.class.getName(),
                "control.for",
                org.openl.binding.impl.ForNodeBinder.class.getName(),
                "control.if",
                org.openl.binding.impl.IfNodeBinder.class.getName(),
                "control.while",
                org.openl.binding.impl.WhileNodeBinder.class.getName(),
                "control.return",
                org.openl.binding.impl.ReturnNodeBinder.class.getName() };

        for (int i = 0; i < binders.length / 2; i++) {
            SingleBinderFactory sbf = new SingleBinderFactory();
            sbf.setNode(binders[2 * i]);
            sbf.setClassName(binders[2 * i + 1]);
            nbc.addConfiguredBinder(sbf);
        }

        LibraryFactoryConfiguration lfc = op.createLibraries();
        NameSpacedLibraryConfiguration nslc = new NameSpacedLibraryConfiguration();
        nslc.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
        JavaLibraryConfiguration javalib = new JavaLibraryConfiguration(
            org.openl.binding.impl.Operators.class.getName());
        nslc.addJavalib(javalib);
        JavaLibraryConfiguration javalib2 = new JavaLibraryConfiguration(
            org.openl.binding.impl.operator.Comparison.class.getName());
        nslc.addJavalib(javalib2);
        lfc.addConfiguredLibrary(nslc);

        /*
         * <libraries> <library namespace="org.openl.operators"> <javalib classname="org.openl.binding.impl.Operators"/>
         * </library> </libraries>
         */

        TypeFactoryConfiguration types = op.createTypes();
        NameSpacedTypeConfiguration nstc = new NameSpacedTypeConfiguration();
        nstc.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        JavaTypeConfiguration javatype = new JavaTypeConfiguration();
        javatype.setClassName(org.openl.types.java.JavaPrimitiveTypeLibrary.class.getName());
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

        JavaLongNameTypeConfiguration javaLongNameType = new JavaLongNameTypeConfiguration();
        nstc.addConfiguration(javaLongNameType);

        types.addConfiguredTypeLibrary(nstc);

        /*
         *
         * <types> <typelibrary namespace="org.openl.this"> <javatype
         * classname="org.openl.types.java.JavaPrimitiveTypeLibrary"/> <javatype
         * classname="org.openl.types.java.JavaLang"/> <javaimport> <import>java.util</import> </javaimport>
         * </typelibrary> </types>
         */

        TypeCastFactory typecast = op.createTypecast();
        TypeCastFactory.JavaCastComponent javacast = typecast.new JavaCastComponent(
            org.openl.binding.impl.cast.CastOperators.class.getName(),
            org.openl.binding.impl.cast.CastFactory.class.getName());
        typecast.addJavaCast(javacast);

        /*
         * <typecast> <javacast libraryclassname="org.openl.binding.impl.Operators"
         * classname="org.openl.binding.impl.ACastFactory"/> </typecast>
         */

        return op;

    }

}

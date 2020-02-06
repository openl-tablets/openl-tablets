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
                "module.top",
                org.openl.binding.impl.module.ModuleNodeBinder.class.getName(),
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
                "where.var.expalnation",
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

        javatype = new JavaTypeConfiguration();
        javatype.setClassName(org.openl.types.java.JavaLang.class.getName());
        nstc.addConfiguration(javatype);

        JavaLongNameTypeConfiguration javaLongNameType = new JavaLongNameTypeConfiguration();
        nstc.addConfiguration(javaLongNameType);

        JavaImportTypeConfiguration javaimport = new JavaImportTypeConfiguration();

        javaimport.addPackageImport("java.util");
        javaimport.addPackageImport("java.math");

        nstc.addConfiguration(javaimport);

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

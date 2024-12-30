package org.openl.binding;

import java.util.HashMap;

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
import org.openl.binding.impl.NotExistNodeBinder;
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
import org.openl.binding.impl.ce.MethodNodeBinder;
import org.openl.binding.impl.module.MethodDeclarationNodeBinder;
import org.openl.binding.impl.module.MethodParametersNodeBinder;
import org.openl.binding.impl.module.ParameterDeclarationNodeBinderWithContextParameterSupport;
import org.openl.binding.impl.module.VarDeclarationNodeBinder;

/**
 * A set of binders which are registered in a hierarchy manner. The hierarchy is defined by a dotted separated string.
 * So if a binder is registered by an 'op.binary' node string, then an 'op.binary.lt' will also be found.
 *
 * @author Yury Molchan
 */
public class NodeBinders {

    private static final HashMap<String, INodeBinder> BINDERS = new HashMap<>();

    static {
        BINDERS.put("literal", new LiteralNodeBinder());
        BINDERS.put("literal.integer", new IntNodeBinder());
        BINDERS.put("literal.real", new DoubleNodeBinder());
        BINDERS.put("literal.percent", new PercentNodeBinder());
        BINDERS.put("literal.string", new StringNodeBinder());
        BINDERS.put("literal.char", new CharNodeBinder());
        BINDERS.put("literal.integer.business", new BusinessIntNodeBinder());
        BINDERS.put("array.init", new ArrayInitializationBinder());
        BINDERS.put("method.header", new MethodHeaderNodeBinder());
        BINDERS.put("param.declaration", new ParameterDeclarationNodeBinder());
        BINDERS.put("method.parameters", new MethodParametersNodeBinder());
        BINDERS.put("method.declaration", new MethodDeclarationNodeBinder());
        BINDERS.put("var.declaration", new VarDeclarationNodeBinder());
        BINDERS.put("parameter.declaration", new ParameterDeclarationNodeBinderWithContextParameterSupport());
        BINDERS.put("block", new BlockBinder());
        BINDERS.put("op.binary", new BinaryOperatorNodeBinder());
        BINDERS.put("op.binary.and", new BinaryOperatorAndNodeBinder());
        BINDERS.put("op.binary.or", new BinaryOperatorOrNodeBinder());
        BINDERS.put("op.unary", new UnaryOperatorNodeBinder());
        BINDERS.put("op.prefix", new PrefixOperatorNodeBinder());
        BINDERS.put("op.suffix", new SuffixOperatorNodeBinder());
        BINDERS.put("op.assign", new AssignOperatorNodeBinder());
        BINDERS.put("op.new.object", new NewNodeBinder());
        BINDERS.put("op.new.array", new NewArrayNodeBinder());
        BINDERS.put("op.index", new IndexNodeBinder());
        BINDERS.put("selectfirst.index", new SelectFirstIndexNodeBinder());
        BINDERS.put("selectall.index", new SelectAllIndexNodeBinder());
        BINDERS.put("orderby.index", new OrderByIndexNodeBinder());
        BINDERS.put("orderdecreasingby.index", new OrderByIndexNodeBinder());
        BINDERS.put("splitby.index", new SplitByIndexNodeBinder());

        BINDERS.put("transform.index", new TransformIndexNodeBinder());
        BINDERS.put("transformunique.index", new TransformIndexNodeBinder());
        BINDERS.put("index.parameter.declaration", new IndexParameterDeclarationBinder());

        BINDERS.put("op.ternary.qmark", new IfNodeBinderWithCSRSupport());
        BINDERS.put("type.cast", new TypeCastBinder());
        BINDERS.put("local.var.declaration", new LocalVarBinder());
        BINDERS.put("type.declaration", new TypeBinder());
        BINDERS.put("function", new MethodNodeBinder());
        BINDERS.put("identifier", new IdentifierBinder());
        BINDERS.put("identifier.sequence", new IdentifierSequenceBinder());
        BINDERS.put("range.variable", new RangeVariableBinder());
        BINDERS.put("chain", new BExChainBinder());
        BINDERS.put("chain.suffix", new BExChainSuffixBinder());
        BINDERS.put("where.expression", new WhereExpressionNodeBinder());
        BINDERS.put("where.var.explanation", new WhereVarNodeBinder());
        BINDERS.put("list", new ListNodeBinder());
        BINDERS.put("control.for", new ForNodeBinder());
        BINDERS.put("control.if", new IfNodeBinder());
        BINDERS.put("control.while", new WhileNodeBinder());
        BINDERS.put("control.return", new ReturnNodeBinder());
    }

    public static INodeBinder get(String node) {
        var nodeBinder = BINDERS.get(node);
        if (nodeBinder != null) {
            // return an existed binder
            return nodeBinder;
        }
        int lastDot = node.lastIndexOf('.');
        if (lastDot == -1) {
            // no parent node can be extracted, so exit
            return NotExistNodeBinder.the;
        }
        node = node.substring(0, lastDot); // a parent node
        nodeBinder = get(node); // try to get the parent binder
        if (nodeBinder != null) {
            // store the parent node to speed up the search
            BINDERS.put(node, nodeBinder);
        }
        return nodeBinder;
    }
}

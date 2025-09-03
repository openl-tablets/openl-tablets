package org.bool.expr

import com.bpodgursky.jbool_expressions.*
import com.bpodgursky.jbool_expressions.rules.RuleSet
import org.apache.commons.lang3.tuple.Triple
import org.openl.binding.IBoundNode
import org.openl.binding.impl.*
import org.openl.rules.operator.*
import org.openl.rules.convertor.IString2DataConvertor
import org.openl.rules.convertor.String2DataConvertorFactory
import org.openl.rules.dt.AST
import org.openl.types.IOpenClass
import org.openl.types.IOpenMethod
import org.openl.types.java.JavaOpenClass
import org.openl.vm.SimpleRuntimeEnv

import java.util.function.Supplier
import java.util.stream.Collectors

/**
 *  Not covered cases:
 *
 *  1) a <= 10, a < 10 or a == 10, The solution doesn't merge the case.
 *  2) Division is not fully supported.
 *
 *  The algorithm can be improved to cover this cases but effort for this case has not been approved.*/
class ExprTool {
    private static final IString2DataConvertor STRING_TO_DOUBLE_CONVERTOR = String2DataConvertorFactory.getConvertor(Double.class)
    private static final IOpenMethod MUL_OP_METHOD = JavaOpenClass.getOpenClass(Operators.class).getMethod("multiply", new IOpenClass[]{JavaOpenClass.DOUBLE, JavaOpenClass.DOUBLE})
    private static final IOpenMethod ADD_OP_METHOD = JavaOpenClass.getOpenClass(Operators.class).getMethod("add", new IOpenClass[]{JavaOpenClass.DOUBLE, JavaOpenClass.DOUBLE})
    private static final EMPTY_SE_ARRAY = new SE[]{}
    private static final Double ZERO = 0.0d
    private static final Double ONE = 1.0d

    private static Double mul(Double a, Double b) {
        return (Double) MUL_OP_METHOD.invoke(null, new Object[]{a, b}, new SimpleRuntimeEnv())
    }

    private static Double add(Double a, Double b) {
        return (Double) ADD_OP_METHOD.invoke(null, new Object[]{a, b}, new SimpleRuntimeEnv())
    }

    private static List<IOpenClass> INTEGER_TYPES = List.of(JavaOpenClass.BYTE,
            JavaOpenClass.getOpenClass(Byte.class),
            JavaOpenClass.SHORT,
            JavaOpenClass.getOpenClass(Short.class),
            JavaOpenClass.INT,
            JavaOpenClass.getOpenClass(Integer.class),
            JavaOpenClass.LONG,
            JavaOpenClass.getOpenClass(Long.class),
            JavaOpenClass.getOpenClass(BigInteger.class))

    private static List<IOpenClass> NUMBER_TYPES = List.of(JavaOpenClass.BYTE,
            JavaOpenClass.getOpenClass(Byte.class),
            JavaOpenClass.SHORT,
            JavaOpenClass.getOpenClass(Short.class),
            JavaOpenClass.INT,
            JavaOpenClass.getOpenClass(Integer.class),
            JavaOpenClass.LONG,
            JavaOpenClass.getOpenClass(Long.class),
            JavaOpenClass.getOpenClass(BigInteger.class),
            JavaOpenClass.FLOAT,
            JavaOpenClass.getOpenClass(Float.class),
            JavaOpenClass.DOUBLE,
            JavaOpenClass.getOpenClass(Double.class),
            JavaOpenClass.getOpenClass(BigDecimal.class))

    static Formula[] split(AST ast) {
        if (ast.getBoundNode() == null) {
            return new Formula[0]
        }
        var expression = parse(ast, ast.getBoundNode())
        var dnf = RuleSet.toDNF(expression)
        List<Formula> expressions = new ArrayList<>()
        if (dnf instanceof Or) {
            var or = (Or<Node>) dnf
            for (Expression<Node> e : or.expressions) {
                var formula = new Formula(exprToSE(ast, e, false).value, exprToSE(ast, e, true).value)
                expressions.add(formula)
            }
        } else {
            expressions.add(new Formula(ast.getCode(ast.getBoundNode()), exprToSE(ast, dnf, true).value))
        }
        return expressions.stream().sorted(Comparator.comparing(Formula::getFormula)).toArray(Formula[]::new)
    }

    private static String removeExtraSpaces(String s) {
        var sb = new StringBuilder()
        char prev = ' ' as char
        boolean skip = false
        for (char c : s.chars()) {
            if (c == '"' as char) {
                skip = !skip
            }
            if (!skip) {
                if (c != ' ' as char || prev != ' ' as char) {
                    sb.append(c)
                }
            } else {
                sb.append(c)
            }
            prev = c
        }
        return sb.toString().trim()
    }

    private static boolean isBooleanType(IOpenClass openClass) {
        return openClass == JavaOpenClass.getOpenClass(Boolean.class) || openClass == JavaOpenClass.BOOLEAN
    }

    private static String getOppositeSwapOperation(String op) {
        switch (op) {
            case ">": return "<"
            case "<": return ">"
            case ">=": return "<="
            case "<=": return ">="
            case "==": return "=="
            case "!=": return "!="
            case "string==": return "string=="
            case "string!=": return "string!="
            case "string>": return "string<"
            case "string<": return "string>"
            case "string>=": return "string<="
            case "string<=": return "string>="
        }
        return null
    }

    private static String getOppositeOperation(String op) {
        switch (op) {
            case ">": return "<="
            case "<": return ">="
            case ">=": return "<"
            case "<=": return ">"
            case "==": return "!="
            case "!=": return "=="
            case "string==": return "string!="
            case "string!=": return "string=="
            case "string>": return "string<="
            case "string<": return "string>="
            case "string>=": return "string<"
            case "string<=": return "string>"
        }
        return null
    }

    private static String getOperation(IOpenMethod openMethod) {
        String name = openMethod.getName()
        switch (name) {
            case "gt": return ">"
            case "lt": return "<"
            case "ge": return ">="
            case "le": return "<="
            case "eq": return "=="
            case "ne": return "!="
            case "string_eq": return "string=="
            case "string_ne": return "string!="
            case "string_gt": return "string>"
            case "string_lt": return "string<"
            case "string_ge": return "string>="
            case "string_le": return "string<="
        }
        return null
    }

    private static int getOperationPriority(String op) {
        //https://en.cppreference.com/w/c/language/operator_precedence
        if (op == "not" || op == "negative" || op == "cast") {
            return 2
        } else if (op == "*" || op == "/" || op == "%") {
            return 3
        } else if (op == "+" || op == "-") {
            return 4
        } else if (op == ">" || op == ">=" || op == "<" || op == "<=" || op == "string<" || op == "string<=" || op == "string>" || op == "string>=") {
            return 6
        } else if (op == "==" || op == "!=" || op == "string!=" || op == "string==") {
            return 7
        } else if (op == "and") {
            return 11
        } else if (op == "or") {
            return 12
        } else if (op == "?" || op == "?:") {
            return 13
        }
        throw new IllegalStateException()
    }

    private static Triple<Boolean, String, String> canSimplify(String op, Expression<Node> expression, Expression<Node> otherExpression) {
        if (("<=" == op || ">=" == op) && isLiteral(expression) && otherExpression instanceof Variable && INTEGER_TYPES.contains(((Variable<Node>) otherExpression).value.boundNode.getType())) {
            Object value = getLiteralValue(expression)
            if (value instanceof Byte) {
                Byte v = (Byte) value
                boolean f = "<=" == op && v != Byte.MAX_VALUE || ">=" == op && v != Byte.MIN_VALUE
                return Triple.of(f, String.valueOf("<=" == op ? v + 1 : v - 1), "<=" == op ? "<" : ">")
            } else if (value instanceof Short) {
                Short v = (Short) value
                boolean f = "<=" == op && v != Short.MAX_VALUE || ">=" == op && v != Short.MIN_VALUE
                return Triple.of(f, String.valueOf("<=" == op ? v + 1 : v - 1), "<=" == op ? "<" : ">")
            } else if (value instanceof Integer) {
                Integer v = (Integer) value
                boolean f = "<=" == op && v != Integer.MAX_VALUE || ">=" == op && v != Integer.MIN_VALUE
                return Triple.of(f, String.valueOf("<=" == op ? v + 1 : v - 1), "<=" == op ? "<" : ">")
            } else if (value instanceof Long) {
                Long v = (Long) value
                boolean f = "<=" == op && v != Long.MAX_VALUE || ">=" == op && v != Long.MIN_VALUE
                return Triple.of(f, String.valueOf("<=" == op ? v + 1 : v - 1), "<=" == op ? "<" : ">")
            } else if (value instanceof BigInteger) {
                BigInteger v = (BigInteger) value
                return Triple.of(Boolean.TRUE, String.valueOf("<=" == op ? v.add(BigInteger.ONE) : v.add(BigInteger.ONE.negate())), "<=" == op ? "<" : ">")
            }
        }
        return Triple.of(Boolean.FALSE, null, null)
    }

    private static String fieldBoundNodeToString(AST ast, FieldBoundNode fieldBoundNode) {
        String s = null
        IBoundNode b = fieldBoundNode
        boolean f = false
        while (b instanceof FieldBoundNode || b instanceof IndexNode) {
            if (b instanceof FieldBoundNode) {
                FieldBoundNode fieldBoundNode1 = (FieldBoundNode) b
                s = (s == null) ? fieldBoundNode1.getBoundField().getName() : fieldBoundNode1.getBoundField().getName() + (f ? "." : "") + s
                f = true
            } else if (b instanceof IndexNode) {
                IBoundNode indexBoundNode = ((IndexNode) b).getChildren()[0]
                String d = "[" + boundNodeToSE(ast, indexBoundNode, true).value + "]"
                s = (s == null) ? d : d + (f ? "." : "") + s
                f = false
            }
            b = b.getTargetNode()
        }
        return b == null ? s : removeExtraSpaces(ast.getCode(fieldBoundNode))
    }

    private static boolean isOppositeOperationPrefer(String operation) {
        return operation != "==" && operation.contains("=")
    }

    private static Term[] toSumTerms(AST ast, IBoundNode boundNode, boolean formula) {
        if (isSumBoundNode(boundNode)) {
            return sumBoundNodeToSumTerms(ast, (BinaryOpNode) boundNode, formula)
        }
        return new Term[]{new Term(new SE[]{boundNodeToSE(ast, boundNode, formula)}, EMPTY_SE_ARRAY)}
    }

    private static Term[] toMulTerms(AST ast, IBoundNode boundNode, boolean formula) {
        if (isMulBoundNode(boundNode)) {
            return mulBoundNodeToSE(ast, (BinaryOpNode) boundNode, formula)
        }
        SE se = boundNodeToSE(ast, boundNode, formula)
        if (se.terms != null) {
            return se.terms
        }
        SE[] l = new SE[]{boundNodeToSE(ast, boundNode, formula)}
        return new Term[]{new Term(l, EMPTY_SE_ARRAY)}
    }

    private static SE negativeSE(SE e) {
        if (e.inverted != null) {
            return e.inverted.get()
        } else {
            SE se = new SE("-" + e.value, e.determined, "negative", e.literal)
            se.inverted = () -> e
            return se
        }
    }

    private static SE[] negativeSEs(SE[] ses) {
        return Arrays.stream(ses).map(e -> negativeSE(e)).toArray(SE[]::new)
    }

    private static Term negativeTerm(Term term) {
        SE e = negativeSE(term.left[0])
        Term t = term.clone()
        t.left[0] = e
        return t
    }

    private static Term[] negativeTerms(Term[] terms) {
        return Arrays.stream(terms).map(e -> negativeTerm(e)).toArray(Term[]::new)
    }

    private static List<SE> pushLiteralsToStack(SE[] terms, Stack<Double> stack) {
        List<SE> nonLiteralTerms = new ArrayList<>();
        for (SE se : terms) {
            if (se != null) {
                try {
                    stack.push(STRING_TO_DOUBLE_CONVERTOR.parse(se.value, null))
                } catch (Exception ignored) {
                    nonLiteralTerms.add(se)
                }
            }
        }
        return nonLiteralTerms;
    }

    private static boolean isNegativeSE(SE se) {
        return se.operation == "negative"
    }

    private static boolean invertNegativeSes(SE[] ses) {
        boolean negative = false
        for (int i = 0; i < ses.length; i++) {
            if (ses[i] != null) {
                if (isNegativeSE(ses[i])) {
                    ses[i] = negativeSE(ses[i])
                    negative = !negative
                }
            }
        }
        return negative
    }

    private static SE[] simplifySumSes(SE[] ses) {
        if (ses == null) {
            return null
        }
        List<SE> x = new ArrayList<>()
        for (int i = 0; i < ses.length - 1; i++) {
            if (ses[i] != null && ses[i].determined) {
                SE x1 = ses[i]
                boolean negative1 = false
                if (isNegativeSE(x1)) {
                    x1 = negativeSE(x1)
                    negative1 = true
                }
                Double v1 = x1.scalar == null ? ONE : x1.scalar
                if (negative1) {
                    v1 = -v1
                }
                String value1 = x1.scalar == null ? x1.value : x1.noScalarValue
                boolean f = false
                for (int j = i + 1; j < ses.length; j++) {
                    if (ses[j] != null && ses[j].determined) {
                        SE x2 = ses[j]
                        boolean negative2 = false
                        if (isNegativeSE(x2)) {
                            x2 = negativeSE(x2)
                            negative2 = true
                        }
                        Double v2 = x2.scalar == null ? ONE : x2.scalar
                        if (negative2) {
                            v2 = -v2
                        }
                        String value2 = x2.scalar == null ? x2.value : x2.noScalarValue
                        if (value1 == value2) {
                            v1 = add(v1, v2)
                            ses[i] = null
                            ses[j] = null
                            f = true
                        }
                    }
                }
                if (f && v1 != ZERO) {
                    SE s1
                    if (value1 != null) {
                        s1 = new SE(String.valueOf(v1) + " * " + value1, true, "*", false)
                        s1.noScalarValue = value1
                    } else {
                        s1 = new SE(String.valueOf(v1), true, null, true)
                    }
                    s1.terms = x1.terms
                    s1.scalar = v1
                    x.add(s1)
                }
            }
        }
        for (SE se : ses) {
            if (se != null) {
                x.add(se)
            }
        }
        if (x.isEmpty()) {
            return new SE("0", true, null, true)
        }
        ses = sort(x as SE[])
        Stack<Double> stack = new Stack<>()
        List<SE> simplifiedTerms = pushLiteralsToStack(ses, stack)
        while (stack.size() > 1) {
            stack.push(add(stack.pop(), stack.pop()))
        }
        if (!stack.isEmpty()) {
            Double v = stack.pop()
            simplifiedTerms.add(new SE(String.valueOf(v), true, null, true))
        }
        if (!simplifiedTerms.isEmpty()) {
            return simplifiedTerms as SE[]
        } else {
            return new SE[]{new SE("0", true, null, true)}
        }
    }

    private static Term simplifyTerm(Term term) {
        SE[] leftSes = sort(term.left)
        SE[] rightSes = sort(term.right)
        boolean negative = false
        for (int i = 0; i < rightSes.length; i++) {
            if (rightSes[i] != null && rightSes[i].determined) {
                for (int j = 0; j < leftSes.length; j++) {
                    if (leftSes[j] != null && leftSes[j].determined) {
                        if (rightSes[i] == leftSes[j]) {
                            rightSes[i] = null
                            leftSes[j] = null
                            break
                        }
                        SE t = negativeSE(rightSes[i])
                        if (t == leftSes[j]) {
                            rightSes[i] = null
                            leftSes[j] = null
                            negative = !negative
                        }
                    }
                }
            }
        }
        negative = negative ^ invertNegativeSes(rightSes)
        negative = negative ^ invertNegativeSes(leftSes)
        Stack<Double> leftStack = new Stack<>()
        List<SE> simplifiedLeftTerms = pushLiteralsToStack(leftSes, leftStack)
        Stack<Double> rightStack = new Stack<>()
        List<SE> simplifiedRightTerms = pushLiteralsToStack(rightSes, rightStack)

        while (leftStack.size() > 1) {
            leftStack.push(mul(leftStack.pop(), leftStack.pop()))
        }
        while (rightStack.size() > 1) {
            rightStack.push(mul(rightStack.pop(), rightStack.pop()))
        }

        SE additionalLeftTerm = null;
        if (!leftStack.isEmpty()) {
            Double v = leftStack.pop()
            if (v == ZERO) {
                SE se = new SE("0", true, null, true)
                se.scalar = 0
                return new Term(new SE[]{se}, EMPTY_SE_ARRAY)
            } else {
                if (simplifiedLeftTerms.isEmpty()) {
                    additionalLeftTerm = new SE(String.valueOf(v), true, null, true)
                    additionalLeftTerm.scalar = v
                } else if (v != ONE) {
                    additionalLeftTerm = new SE(String.valueOf(v), true, null, true)
                    additionalLeftTerm.scalar = v
                }
            }
        }

        SE additionalRightTerm = null;
        if (!rightStack.isEmpty()) {
            Object v = rightStack.pop()
            if (v != ONE) {
                if (v == ZERO) {
                    simplifiedRightTerms.clear()
                }
                additionalRightTerm = new SE(String.valueOf(v), true, null, true)
                additionalRightTerm.scalar = v
            }
        }
        if (additionalRightTerm != additionalLeftTerm) {
            if (additionalLeftTerm != null && additionalRightTerm != null && additionalLeftTerm.scalar != null && additionalRightTerm.scalar != null) {
                Double v = mul(additionalLeftTerm.scalar, additionalRightTerm.scalar)
                SE se = new SE(String.valueOf(v), true, null, true)
                se.scalar = v
                simplifiedLeftTerms.add(se)
            } else {
                if (additionalLeftTerm != null) {
                    simplifiedLeftTerms.add(additionalLeftTerm)
                }
                if (additionalRightTerm != null) {
                    simplifiedRightTerms.add(additionalRightTerm)
                }
            }
        }
        if (simplifiedLeftTerms.isEmpty()) {
            simplifiedLeftTerms.add(new SE("1", true, null, true))
        }
        if (negative) {
            simplifiedLeftTerms.set(0, negativeSE(simplifiedLeftTerms.get(0)))
        }
        return new Term(simplifiedLeftTerms as SE[], simplifiedRightTerms as SE[])
    }

    private static SE buildSE(Term[] terms) {
        terms = Arrays.stream(terms).map(ExprTool::simplifyTerm).toArray(Term[]::new)
        List<SE> ses = new ArrayList<>()
        for (Term term : terms) {
            SE se
            if (term.right.length == 0) {
                se = buildMulSE(term.left)
            } else {
                SE mulSE1 = buildMulSE(term.left)
                SE divSE = buildMulSE(term.right)
                boolean w1 = mulSE1.operation != null && getOperationPriority(mulSE1.operation) > getOperationPriority("/")
                boolean w2 = divSE.operation != null && getOperationPriority(divSE.operation) > getOperationPriority("/")
                String p1 = (w1 ? "(" : "") + mulSE1.value + (w1 ? ")" : "")
                String p2 = (w2 ? "(" : "") + divSE.value + (w2 ? ")" : "")
                se = new SE(p1 + " / " + p2, mulSE1.determined && divSE.determined, "/", false)
                if (mulSE1.scalar != null) {
                    se.scalar = mulSE1.scalar
                    String d1 = (w1 ? "(" : "") + mulSE1.noScalarValue + (w1 ? ")" : "")
                    String d2 = (w2 ? "(" : "") + divSE.value + (w2 ? ")" : "")
                    se.noScalarValue = d1 + " / " + d2
                }
            }
            se.terms = new Term[]{term}
            ses.add(se)
        }
        SE[] ses1 = simplifySumSes(ses as SE[])
        SE ret
        if (ses1.length != 0) {
            if (ses1.length > 1) {
                SE se = new SE(concatSumSesToString(ses1), Arrays.stream(ses1).allMatch(e -> e.determined), "+", false)
                se.inverted = () -> new SE(concatSumSesToString(negativeSEs(ses1)), Arrays.stream(ses1).allMatch(e -> e.determined), "+", false)
                ret = se
            } else {
                ret = new SE(ses1[0].value, ses1[0].determined, ses1[0].operation, ses1[0].literal)
            }
        } else {
            ret = new SE("0", true, null, true)
        }
        ret.terms = terms
        return ret
    }

    private static SE buildMulSE(SE[] ses) {
        if (ses.length > 1) {
            StringBuilder sb = new StringBuilder()
            boolean determined = true
            Double v = null
            for (SE se : ses) {
                if (se.scalar == null) {
                    if (sb.length() != 0) {
                        sb.append(" * ")
                    }
                    if (se.operation != null && getOperationPriority(se.operation) > getOperationPriority("*")) {
                        sb.append("(").append(se.value).append(")")
                    } else {
                        sb.append(se.value)
                    }
                } else {
                    v = se.scalar
                }
                determined = determined && se.determined
            }
            if (v != null) {
                SE se = new SE(String.valueOf(v) + " * " + sb.toString(), determined, "*", false)
                se.scalar = v
                se.noScalarValue = sb.toString()
                return se
            } else {
                return new SE(sb.toString(), determined, "*", false)
            }
        } else {
            return ses[0]
        }
    }

    private static String concatSumSesToString(SE[] ses) {
        StringBuilder sb = new StringBuilder()
        for (SE se : ses) {
            if (sb.length() == 0) {
                sb.append(se.value)
            } else {
                if (se.operation == "negative") {
                    sb.append(" - ")
                    SE se1 = se.inverted.get()
                    sb.append(se1.value)
                } else {
                    sb.append(" + ")
                    sb.append(se.value)
                }
            }
        }
        return sb.toString()
    }

    private static Term[] sumBoundNodeToSumTerms(AST ast, BinaryOpNode binaryOpNode, boolean formula) {
        IOpenMethod method = binaryOpNode.getMethodCaller().getMethod()
        if (isSumBoundNode(binaryOpNode)) {
            Term[] ses1 = toSumTerms(ast, binaryOpNode.getChildren()[0], formula)
            Term[] ses2 = toSumTerms(ast, binaryOpNode.getChildren()[1], formula)
            return (method.getName() == "add" ? ses1 + ses2 : ses1 + negativeTerms(ses2)) as Term[]
        }
        throw new IllegalStateException()
    }

    private static Term[] mulTerms(Term[] terms1, Term[] terms2) {
        List<Term> terms = new ArrayList<Term>()
        for (Term term1 : terms1) {
            for (Term term2 : terms2) {
                terms.add(new Term((term1.left + term2.left) as SE[], (term1.right + term2.right) as SE[]))
            }
        }
        return terms as Term[]
    }

    private static Term[] divTerms(Term[] terms1, Term[] terms2) {
        List<Term> terms = new ArrayList<Term>()
        if (terms2.length > 1) {
            SE[] se = new SE[]{buildSE(terms2)}
            for (Term term1 : terms1) {
                terms.add(new Term(term1.left, (term1.right + se) as SE[]))
            }
        } else {
            for (Term term1 : terms1) {
                terms.add(new Term((term1.left + terms2[0].right) as SE[], (term1.right + terms2[0].left) as SE[]))
            }
        }
        return terms as Term[]
    }

    private static Term[] mulBoundNodeToSE(AST ast, BinaryOpNode binaryOpNode, boolean formula) {
        IOpenMethod method = binaryOpNode.getMethodCaller().getMethod()
        if (isMulBoundNode(binaryOpNode)) {
            Term[] ses1 = toMulTerms(ast, binaryOpNode.getChildren()[0], formula)
            Term[] ses2 = toMulTerms(ast, binaryOpNode.getChildren()[1], formula)
            if (method.getName() == "multiply") {
                return mulTerms(ses1, ses2)
            } else if (method.getName() == "divide") {
                return divTerms(ses1, ses2)
            }
        }
        throw new IllegalStateException()
    }

    private static boolean isSumBoundNode(IBoundNode boundNode) {
        if (boundNode instanceof BinaryOpNode) {
            BinaryOpNode binaryOpNode = (BinaryOpNode) boundNode
            IOpenMethod method = binaryOpNode.getMethodCaller().getMethod()
            return method.getDeclaringClass() == JavaOpenClass.getOpenClass(Operators.class) && method.getSignature().getNumberOfParameters() == 2 && (method.getName() == "add" || method.getName() == "subtract")
        }
        return false
    }

    private static boolean isMulBoundNode(IBoundNode boundNode) {
        if (boundNode instanceof BinaryOpNode) {
            BinaryOpNode binaryOpNode = (BinaryOpNode) boundNode
            IOpenMethod method = binaryOpNode.getMethodCaller().getMethod()
            return method.getDeclaringClass() == JavaOpenClass.getOpenClass(Operators.class) && method.getSignature().getNumberOfParameters() == 2 && (method.getName() == "multiply" || method.getName() == "divide")
        }
        return false
    }

    private static boolean isOperationBoundNode(IBoundNode boundNode) {
        return isSumBoundNode(boundNode) || isMulBoundNode(boundNode)
    }

    private static SE[] copy(SE[] ses) {
        if (ses == null) {
            return null
        }
        SE[] newSes = new SE[ses.length]
        System.arraycopy(ses, 0, newSes, 0, ses.length)
        return newSes
    }

    private static SE[] sort(SE[] ses) {
        if (ses == null) {
            return null
        }
        SE[] newSes = copy(ses)
        Arrays.sort(newSes, (SE e1, SE e2) -> {
            String v = e1.value
            if (e1.operation == "negative") {
                v = e1.inverted.get().value
            }
            if (e2.operation == "negative") {
                e2 = e2.inverted.get()
            }
            return v <=> e2.value
        })
        return newSes
    }

    private static SE boundNodeToSE(AST ast, IBoundNode boundNode, boolean formula) {
        if (formula) {
            if (boundNode instanceof BinaryOpNode) {
                BinaryOpNode binaryOpNode = (BinaryOpNode) boundNode
                IOpenMethod method = binaryOpNode.getMethodCaller().getMethod()
                if (isBooleanType(binaryOpNode.getType())) {
                    if (method.getDeclaringClass() == JavaOpenClass.getOpenClass(Comparison.class) && method.getSignature().getNumberOfParameters() == 2) {
                        Expression<Node> e0 = parse(ast, binaryOpNode.children[0])
                        Expression<Node> e1 = parse(ast, binaryOpNode.children[1])
                        String op = getOperation(binaryOpNode.getMethodCaller().getMethod())
                        if (op == "==" || op == "!=") {
                            if (e0 instanceof Literal && e1 instanceof Literal) {
                                Literal literal0 = (Literal) e0
                                Literal literal1 = (Literal) e1
                                Object v = method.invoke(null, new Object[]{literal0.getValue(),
                                        literal1.getValue()}, new SimpleRuntimeEnv())
                                return new SE(String.valueOf(v), true, null, true)
                            }
                            if (e0 instanceof Literal && !(e1 instanceof Literal)) {
                                Literal literal0 = (Literal) e0
                                return op == "==" && literal0.getValue() || op == "!=" && !literal0.getValue() ? exprToSE(ast, e1, formula) : exprToSE(ast, RuleSet.simplify(Not.of(e1)), formula)
                            }
                            if (!(e0 instanceof Literal) && e1 instanceof Literal) {
                                Literal literal1 = (Literal) e1
                                return op == "==" && literal1.getValue() || op == "!=" && !literal1.getValue() ? exprToSE(ast, e0, formula) : exprToSE(ast, RuleSet.simplify(Not.of(e0)), formula)
                            }
                        }
                        boolean isLiteral0 = isLiteral(e0)
                        boolean isLiteral1 = isLiteral(e1)
                        if (isLiteral0 && !isLiteral1 && op != null) {
                            String op1 = getOppositeSwapOperation(op)
                            Triple<Boolean, String, String> d = canSimplify(op1, e0, e1)
                            if (d.getLeft()) {
                                SE se = exprToSE(ast, e1, formula)
                                SE se1 = new SE(se.determined, d.getRight(), se.value, d.getMiddle())
                                se1.inverted = () -> new SE(se.determined, getOppositeSwapOperation(d.getRight()), se.value, String.valueOf(getLiteralValue(e0)))
                                return se1
                            }
                            String oppositeOp1 = getOppositeOperation(op1)
                            Triple<Boolean, String, String> d1 = canSimplify(oppositeOp1, e0, e1)
                            if (d1.getLeft()) {
                                SE se = exprToSE(ast, e1, formula)
                                SE se1 = new SE(se.determined, op1, se.value, String.valueOf(getLiteralValue(e0)))
                                se1.inverted = () -> new SE(se.determined, d1.getRight(), se.value, d1.getMiddle())
                                return se1
                            }
                        }
                        if (!isLiteral0 && isLiteral1 && op != null) {
                            Triple<Boolean, String, String> d = canSimplify(op, e1, e0)
                            if (d.getLeft()) {
                                SE se = exprToSE(ast, e0, formula)
                                SE se1 = new SE(se.determined, d.getRight(), se.value, d.getMiddle())
                                se1.inverted = () -> new SE(se.determined, getOppositeSwapOperation(d.getRight()), se.value, String.valueOf(getLiteralValue(e1)))
                                return se1
                            }
                            String oppositeOp = getOppositeOperation(op)
                            Triple<Boolean, String, String> d1 = canSimplify(oppositeOp, e1, e0)
                            if (d1.getLeft()) {
                                SE se = exprToSE(ast, e0, formula)
                                SE se1 = new SE(se.determined, op, se.value, String.valueOf(getLiteralValue(e1)))
                                se1.inverted = () -> new SE(se.determined, d1.getRight(), se.value, d1.getMiddle())
                                return se1
                            }
                        }
                        if ((op == "!=" || op == "==") && e0 instanceof Variable && e1 instanceof Variable && e0 == e1) {
                            var v0 = ((Variable<Node>) e0).value.boundNode
                            var v1 = ((Variable<Node>) e1).value.boundNode
                            if (v0 instanceof FieldBoundNode && v1 instanceof FieldBoundNode) {
                                return exprToSE(ast, Literal.of(op == "=="), formula)
                            }
                        }
                        if (op != null) {
                            if ((op == "==" || op == "!=") && e0 instanceof Not && e1 instanceof Not) {
                                e0 = RuleSet.simplify(Not.of(e0))
                                e1 = RuleSet.simplify(Not.of(e1))
                                isLiteral0 = isLiteral(e0)
                                isLiteral1 = isLiteral(e1)
                            } else if (e0 instanceof Not || e1 instanceof Not) {
                                if (e0 instanceof Not) {
                                    e0 = RuleSet.simplify(Not.of(e0))
                                    isLiteral0 = isLiteral(e0)
                                } else {
                                    e1 = RuleSet.simplify(Not.of(e1))
                                    isLiteral1 = isLiteral(e1)
                                }
                                op = op == "==" ? "!=" : "=="
                            }
                            SE se1 = exprToSE(ast, e0, formula)
                            SE se2 = exprToSE(ast, e1, formula)

                            String p1 = se1.value
                            String p2 = se2.value

                            String op1 = se1.operation
                            String op2 = se2.operation
                            if (op == "==" || op == "!=") {
                                if (se1.determined && se2.determined && p1 == p2) {
                                    return exprToSE(ast, Literal.of(op == "=="), formula)
                                } else if (se1.operation != null && se2.operation != null && se1.leftPart == se2.leftPart && se1.rightPart == se2.rightPart && getOppositeOperation(se1.operation) == se2.operation) {
                                    return exprToSE(ast, Literal.of(op != "=="), formula)
                                }
                                boolean l1 = p1 == "true" || p1 == "false"
                                boolean l2 = p2 == "true" || p2 == "false"
                                if (l1 && l2) {
                                    return exprToSE(ast, Literal.of(p1 == p2 ? op == "==" : op == "!="), formula)
                                } else if (l1 && !l2) {
                                    return p1 == "true" ? se2 : exprToSE(ast, RuleSet.simplify(Not.of(e1)), formula)
                                } else if (!l1 && l2) {
                                    return p2 == "true" ? se1 : exprToSE(ast, RuleSet.simplify(Not.of(e0)), formula)
                                }
                                if (se1.operation != null && isOppositeOperationPrefer(se1.operation)) {
                                    op = getOppositeOperation(op)
                                    op1 = getOppositeOperation(op1)
                                    //Inverting operation doesn't change its priority and no need to manipulate with bracers
                                    p1 = se1.leftPart + " " + op1 + " " + se1.rightPart
                                }
                                if (se2.operation != null && isOppositeOperationPrefer(se2.operation)) {
                                    op = getOppositeOperation(op)
                                    op2 = getOppositeOperation(op2)
                                    //Inverting operation doesn't change its priority and no need to manipulate with bracers
                                    p2 = se2.leftPart + " " + op2 + " " + se2.rightPart
                                }
                            }
                            if (p1 < p2 && !(isLiteral0 && !isLiteral1) || !isLiteral0 && isLiteral1) {
                                int dOp = getOperationPriority(op)
                                String leftPart = op1 != null && dOp < getOperationPriority(op1) ? "(" + p1 + ")" : p1
                                String rightPart = op2 != null && dOp <= getOperationPriority(op2) ? "(" + p2 + ")" : p2
                                return new SE(se1.determined && se2.determined, op, leftPart, rightPart)
                            } else {
                                String op11 = getOppositeSwapOperation(op)
                                if (op11 != null) {
                                    int dOp = getOperationPriority(op)
                                    String leftPart = op2 != null && dOp < getOperationPriority(op2) ? "(" + p2 + ")" : p2
                                    String rightPart = op1 != null && dOp <= getOperationPriority(op1) ? "(" + p1 + ")" : p1
                                    return new SE(se1.determined && se2.determined, op11, leftPart, rightPart)
                                }
                            }
                        }
                    }
                } else if (isOperationBoundNode(boundNode)) {
                    IOpenClass type = ((BinaryOpNode) boundNode).getMethodCaller().getMethod().getType()
                    if (NUMBER_TYPES.contains(type)) {
                        Term[] terms
                        if (isSumBoundNode(boundNode)) {
                            terms = sumBoundNodeToSumTerms(ast, binaryOpNode, formula)
                        } else if (isMulBoundNode(boundNode)) {
                            terms = mulBoundNodeToSE(ast, boundNode, formula)
                        } else {
                            throw new IllegalStateException()
                        }
                        return buildSE(terms)
                    } else {
                        Expression<Node> p0 = parse(ast, binaryOpNode.getChildren()[0])
                        Expression<Node> p1 = parse(ast, binaryOpNode.getChildren()[1])
                        if (isLiteral(p0) && isLiteral(p1)) {
                            Object v1 = getLiteralValue(p0)
                            Object v2 = getLiteralValue(p1)
                            Object v = binaryOpNode.getMethodCaller().invoke(null, new Object[]{v1, v2}, new SimpleRuntimeEnv())
                            return new SE("\"" + String.valueOf(v) + "\"", true, null, true)
                        }
                    }
                }
            } else if (boundNode instanceof FieldBoundNode) {
                return new SE(fieldBoundNodeToString(ast, (FieldBoundNode) boundNode), true, null, false)
            } else if (boundNode instanceof LiteralBoundNode) {
                return new SE(formula ? removeExtraSpaces(ast.getCode(boundNode)) : ast.getCode(boundNode), true, null, true)
            } else if (boundNode instanceof TypeCastNode) {
                if (boundNode.getChildren()[0].getType() == boundNode.getType()) {
                    return boundNodeToSE(ast, boundNode.getChildren()[0], formula)
                } else {
                    Expression<Node> expr = parse(ast, boundNode.getChildren()[0])
                    if (isLiteral(expr)) {
                        Object v = ((TypeCastNode) boundNode).getCast().convert(getLiteralValue(expr))
                        return new SE(String.valueOf(v), true, null, true)
                    }
                    SE se = exprToSE(ast, expr, formula)
                    boolean w = se.operation != null && getOperationPriority(se.operation) > getOperationPriority("cast")
                    return new SE("(" + boundNode.getType().getName() + ") " + (w ? "(" : "") + se.value + (w ? ")" : ""), se.determined, se.operation, false)
                }
            } else if (boundNode instanceof MethodBoundNode) {
                MethodBoundNode methodBoundNode = (MethodBoundNode) boundNode
                IOpenMethod method = methodBoundNode.getMethodCaller().getMethod()
                if (isNegativeMethodBoundNode(methodBoundNode)) {
                    int d = 1
                    IBoundNode b = methodBoundNode.getChildren()[0]
                    while (isNegativeMethodBoundNode(b)) {
                        d++
                        b = b.getChildren()[0]
                    }
                    SE se = boundNodeToSE(ast, b, formula)
                    if (d % 2 != 0) {
                        if (se.inverted != null && se.operation == "+") {
                            return se.inverted.get()
                        }
                        SE se1 = new SE("-" + se.value, se.determined, "negative", se.literal)
                        se1.inverted = () -> se
                        return se1
                    } else {
                        return se
                    }
                } else {
                    StringBuilder sb = new StringBuilder()
                    if (methodBoundNode.getTargetNode() != null) {
                        SE se = boundNodeToSE(ast, methodBoundNode.getTargetNode(), formula)
                        sb.append(se.value).append(".")
                    }
                    sb.append(method.getName()).append("(")
                    boolean f = false
                    for (IBoundNode boundNode1 : methodBoundNode.getChildren()) {
                        if (f) {
                            sb.append(", ")
                        }
                        f = true
                        sb.append(boundNodeToSE(ast, boundNode1, formula).value)
                    }
                    sb.append(")")
                    return new SE(sb.toString(), false, null, false)
                }
            }
        }
        if (boundNode.getChildren().length == 1 && isIgnorableBoundNode(boundNode)) {
            return boundNodeToSE(ast, boundNode.getChildren()[0], formula)
        }
        return new SE(formula ? removeExtraSpaces(ast.getCode(boundNode)) : ast.getCode(boundNode), false, null, false)
    }

    private static boolean isNegativeMethodBoundNode(IBoundNode boundNode) {
        if (boundNode instanceof MethodBoundNode) {
            MethodBoundNode methodBoundNode = (MethodBoundNode) boundNode
            IOpenMethod method = methodBoundNode.getMethodCaller().getMethod()
            if (method.getName() == "negative" && method.getDeclaringClass() == JavaOpenClass.getOpenClass(Operators.class) && method.getSignature().getNumberOfParameters() == 1) {
                return true
            }
        }
        return false
    }

    private static SE exprToSE(AST ast, Expression<Node> expression, boolean formula) {
        if (expression instanceof And) {
            var and = (And<Node>) expression
            Set<String> expressions = new HashSet<>()
            boolean f = true
            for (Expression<Node> e : and.expressions) {
                SE se = exprToSE(ast, e, formula)
                expressions.add(se.value)
                f = f && se.determined
            }
            List<String> expressions1 = new ArrayList<>(expressions)
            Collections.sort(expressions1)
            return new SE(expressions1.stream().collect(Collectors.joining(" and ")), f, "and", false)
        } else if (expression instanceof Or) {
            var or = (Or<Node>) expression
            Set<String> expressions = new HashSet<>()
            boolean f = true
            for (Expression<Node> e : or.expressions) {
                SE se = exprToSE(ast, e, formula)
                expressions.add(se.value)
                f = f && se.determined
            }
            List<String> expressions1 = new ArrayList<>(expressions)
            Collections.sort(expressions1)
            return new SE(expressions1.stream().collect(Collectors.joining(" or ")), f, "or", false)
        } else if (expression instanceof Variable) {
            var variable = (Variable<Node>) expression
            if (formula || variable.getValue().supplier != null) {
                if (variable.getValue().supplier != null) {
                    return variable.getValue().supplier.get()
                } else {
                    return variable.getValue().getV()
                }
            } else {
                return boundNodeToSE(ast, variable.getValue().boundNode, formula)
            }
        } else if (expression instanceof Literal) {
            Literal<Node> literal = (Literal<Node>) expression
            return new SE(String.valueOf(literal), true, null, true)
        } else if (expression instanceof Not) {
            Expression e = RuleSet.simplify(Not.of(expression))
            SE se = exprToSE(ast, e, true)
            if (se.value == "true" || se.value == "false") {
                return new SE(se.value == "true" ? exprToSE(ast, Literal.getFalse(), true).value : exprToSE(ast, Literal.getTrue(), true).value, true, null, true)
            }
            if (se.operation != null && formula) {
                if (se.inverted != null) {
                    return se.inverted.get()
                }
                String dOp = getOppositeOperation(se.operation)
                if (dOp != null) {
                    return new SE(se.determined, dOp, se.leftPart, se.rightPart)
                }
            }
            SE se1 = !formula ? exprToSE(ast, e, formula) : se
            boolean w = se.operation != null && getOperationPriority(se.operation) > getOperationPriority("not")
            return new SE("not " + (w ? "(" : "") + se1.value + (w ? ")" : ""), se.determined, "not", false)
        }
        throw new IllegalStateException()
    }

    private static Expression<Node> parse(AST ast, IBoundNode boundNode) {
        if (boundNode instanceof BinaryOpNodeAnd) {
            var binaryOpNodeAnd = (BinaryOpNodeAnd) boundNode
            var parseLeft = parse(ast, binaryOpNodeAnd.getLeft())
            var parseRight = parse(ast, binaryOpNodeAnd.getRight())
            List<Expression<Node>> children = new ArrayList<>()
            if (parseLeft instanceof And) {
                children.addAll(Arrays.asList(((And<Node>) parseLeft).expressions))
            } else {
                children.add(parseLeft)
            }
            if (parseRight instanceof And) {
                children.addAll(Arrays.asList(((And<Node>) parseRight).expressions))
            } else {
                children.add(parseRight)
            }
            return And.of(children)
        } else if (boundNode instanceof BinaryOpNodeOr) {
            var binaryOpNodeOr = (BinaryOpNodeOr) boundNode
            var parseLeft = parse(ast, binaryOpNodeOr.getLeft())
            var parseRight = parse(ast, binaryOpNodeOr.getRight())
            List<Expression<Node>> children = new ArrayList<>()
            if (parseLeft instanceof Or) {
                children.addAll(Arrays.asList(((Or<Node>) parseLeft).expressions))
            } else {
                children.add(parseLeft)
            }
            if (parseRight instanceof Or) {
                children.addAll(Arrays.asList(((Or<Node>) parseRight).expressions))
            } else {
                children.add(parseRight)
            }
            return Or.of(children)
        } else if (isLiteralBoundNode(boundNode)) {
            if (isBooleanType(boundNode.getType())) {
                if (Boolean.TRUE == getLiteralValueBoundNode(boundNode)) {
                    return Literal.getTrue()
                } else if (Boolean.FALSE == getLiteralValueBoundNode(boundNode)) {
                    return Literal.getFalse()
                }
            }
            return buildVariable(ast, boundNode)
        } else if (isNot(boundNode)) {
            return Not.of(parse(ast, boundNode.getChildren()[0]))
        } else if (boundNode instanceof BinaryOpNode) {
            var binaryOpNode = (BinaryOpNode) boundNode
            var method = binaryOpNode.getMethodCaller().getMethod()
            if (isBooleanType(binaryOpNode.getType())) {
                if (("eq" == method.getName() || "ne" == method.getName()) && method.getDeclaringClass() == JavaOpenClass.getOpenClass(Comparison.class) && method.getSignature().getNumberOfParameters() == 2 && isBooleanType(binaryOpNode.getChildren()[0].getType()) && isBooleanType(binaryOpNode.getChildren()[1].getType())) {
                    Expression<Node> p0 = parse(ast, binaryOpNode.getChildren()[0])
                    Expression<Node> p1 = parse(ast, binaryOpNode.getChildren()[1])
                    boolean isLiteral0 = p0 instanceof Literal
                    boolean isLiteral1 = p1 instanceof Literal
                    if (isLiteral0 && isLiteral1) {
                        Literal<Node> literal0 = (Literal<Node>) p0
                        Literal<Node> literal1 = (Literal<Node>) p1
                        Literal<Node> x = Literal<Node>.of(literal0.value && literal1.value)
                        return "eq" == method.getName() ? x : Not.of(x)
                    }
                    if (isLiteral0) {
                        Literal<Node> literal = (Literal<Node>) p0
                        return "eq" == method.getName() && literal.value || "ne" == method.getName() && !literal.value ? p1 : Not.of(p1)
                    }
                    if (isLiteral1) {
                        Literal<Node> literal = (Literal<Node>) p1
                        return "eq" == method.getName() && literal.value || "ne" == method.getName() && !literal.value ? p0 : Not.of(p0)
                    }
                    SE se0 = exprToSE(ast, p0, true)
                    SE se1 = exprToSE(ast, p1, true)
                    if (se0.determined && se1.determined && se0.value == se1.value) {
                        return Literal.of("eq" == method.getName())
                    }
                }
                if (binaryOpNode.getMethodCaller().getMethod().getDeclaringClass() == JavaOpenClass.getOpenClass(Comparison.class) && binaryOpNode.getMethodCaller().getMethod().getSignature().getNumberOfParameters() == 2) {
                    Expression<Node> p0 = parse(ast, binaryOpNode.getChildren()[0])
                    Expression<Node> p1 = parse(ast, binaryOpNode.getChildren()[1])
                    boolean isLiteral0 = isLiteral(p0)
                    boolean isLiteral1 = isLiteral(p1)
                    if (isLiteral0 && isLiteral1) {
                        var literal1 = getLiteralValue(p0)
                        var literal2 = getLiteralValue(p1)
                        if (Boolean.TRUE == boundNode.getMethodCaller().invoke(null, new Object[]{literal1, literal2}, new SimpleRuntimeEnv())) {
                            return Literal.getTrue()
                        } else {
                            return Literal.getFalse()
                        }
                    }
                }
            }
            return buildVariable(ast, boundNode)
        } else if (boundNode.getChildren().length == 1 && isIgnorableBoundNode(boundNode)) {
            return parse(ast, boundNode.getChildren()[0])
        } else {
            return buildVariable(ast, boundNode)
        }
    }

    private static boolean isIgnorableBoundNode(IBoundNode boundNode) {
        return boundNode instanceof BlockNode || boundNode instanceof MethodCastNode
    }

    private static boolean isLiteralBoundNode(IBoundNode boundNode) {
        if (boundNode instanceof LiteralBoundNode) {
            return true
        } else if (boundNode instanceof CastNode) {
            return isLiteralBoundNode(boundNode.getChildren()[0])
        } else if (boundNode instanceof FieldBoundNode) {
            var fieldBoundNode = (FieldBoundNode) boundNode
            return fieldBoundNode.getBoundField().isStatic()
        }
        return false
    }

    private static boolean isLiteral(Expression<Node> expression) {
        if (expression instanceof Literal) {
            return true
        } else if (expression instanceof Variable) {
            Variable<Node> variable = (Variable<Node>) expression
            if (variable.value.v.literal) {
                String s = variable.value.v.value
                IOpenClass openClass = variable.value.boundNode.getType()
                if (JavaOpenClass.STRING != openClass) {
                    IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(openClass.getInstanceClass())
                    try {
                        convertor.parse(s, null)
                        return true
                    } catch (Exception ignored) {
                    }
                } else {
                    return true
                }
            }
            IBoundNode boundNode = variable.value.boundNode
            return isLiteralBoundNode(boundNode)
        }
        return false
    }

    private static Object getLiteralValueBoundNode(IBoundNode boundNode) {
        if (boundNode instanceof LiteralBoundNode) {
            return ((LiteralBoundNode) boundNode).getValue()
        } else if (boundNode instanceof CastNode) {
            var castNode = (CastNode) boundNode
            return castNode.getCast().convert(getLiteralValueBoundNode(castNode.getChildren()[0]))
        } else if (boundNode instanceof FieldBoundNode) {
            var fieldBoundNode = (FieldBoundNode) boundNode
            return fieldBoundNode.getBoundField().get(null, new SimpleRuntimeEnv())
        }
        throw new IllegalStateException()
    }

    private static Object getLiteralValue(Expression<Node> expression) {
        if (expression instanceof Literal) {
            return ((Literal<Node>) expression).getValue()
        } else if (expression instanceof Variable) {
            Variable<Node> variable = (Variable<Node>) expression
            if (variable.value.v.literal) {
                String s = variable.value.v.value
                IOpenClass openClass = variable.value.boundNode.getType()
                if (JavaOpenClass.STRING != openClass) {
                    IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(openClass.getInstanceClass())
                    try {
                        return convertor.parse(s, null)
                    } catch (Exception ignored) {
                    }
                } else {
                    return s.substring(1, s.length() - 1)
                }
            }
            IBoundNode boundNode = variable.value.boundNode
            return getLiteralValueBoundNode(boundNode)
        }
        throw new IllegalStateException()
    }

    private static Expression<Node> buildVariable(AST ast, IBoundNode boundNode) {
        if (boundNode instanceof IfNode) {
            IfNode ifNode = (IfNode) boundNode
            if (isBooleanType(ifNode.getType())) {
                Expression<Node> condition = parse(ast, ifNode.getConditionNode())
                condition = RuleSet.simplify(condition)
                Expression<Node> then = parse(ast, ifNode.getThenNode())
                then = RuleSet.simplify(then)
                if (ifNode.getElseNode() == null) {
                    SE conditionSE = exprToSE(ast, condition, true)
                    if (then instanceof Not) {
                        Expression<Node> d = RuleSet.simplify(Not.of(then))
                        SE thenSE = exprToSE(ast, d, true)
                        SE se = new SE(conditionSE.value + " ? " + thenSE.value, conditionSE.determined && thenSE.determined, "?", false)
                        Node node = new Node(boundNode, se)
                        node.supplier = () -> new SE(exprToSE(ast, condition, false).value + " ? " + exprToSE(ast, d, false).value, conditionSE.determined && thenSE.determined, "?", false)
                        return Not.of(Variable.of(node))
                    }
                    SE thenSE = exprToSE(ast, then, true)
                    SE se = new SE(conditionSE.value + " ? " + thenSE.value, conditionSE.determined && thenSE.determined, "?", false)
                    return Variable.of(new Node(boundNode, se))
                }
                Expression<Node> elseE = parse(ast, boundNode.getElseNode())
                elseE = RuleSet.simplify(elseE)
                boolean not = false
                if (then instanceof Not && elseE instanceof Not) {
                    then = RuleSet.simplify(Not.of(then))
                    elseE = RuleSet.simplify(Not.of(elseE))
                    not = true
                }
                boolean v = false
                if (condition instanceof Not) {
                    condition = RuleSet.simplify(Not.of(condition))
                    var t = elseE
                    elseE = then
                    then = t
                    v = true
                }
                SE conditionSE = exprToSE(ast, condition, true)
                SE thenSE = exprToSE(ast, then, true)
                SE elseSE = exprToSE(ast, elseE, true)
                String s = conditionSE.value + " ? " + thenSE.value + " : " + elseSE.value
                SE se = new SE(s, conditionSE.determined && thenSE.determined && elseSE.determined, "?:", false)
                Node node = new Node(boundNode, se)
                if (v || not) {
                    node.supplier = () -> new SE(exprToSE(ast, condition, false).value + " ? " + exprToSE(ast, then, false).value + " : " + exprToSE(ast, elseE, false).value, conditionSE.determined && thenSE.determined && elseSE.determined, "?:", false)
                }
                Variable<Node> variable = Variable.of(node)
                return not ? Not.of(variable) : variable
            }
        }
        return Variable.of(new Node(boundNode, boundNodeToSE(ast, boundNode, true)))
    }

    private static boolean isNot(IBoundNode boundNode) {
        if (boundNode instanceof UnaryOpNode) {
            var unaryOpNode = (UnaryOpNode) boundNode
            var openMethod = unaryOpNode.getMethodCaller().getMethod()
            return "not" == openMethod.getName() && openMethod.getDeclaringClass() == JavaOpenClass.getOpenClass(Operators.class)
        }
        return false
    }

    static class Term {
        SE[] left
        SE[] right
        boolean determined

        Term(SE[] left, SE[] right) {
            this.left = left
            this.right = right
            this.determined = true
            Arrays.stream(left).forEach(se -> this.determined &= se.determined)
            Arrays.stream(right).forEach(se -> this.determined &= se.determined)
        }

        Term clone() {
            return new Term(left.clone(), right.clone())
        }
    }

    static class SE {
        String value
        boolean determined
        String operation
        String leftPart
        String rightPart
        Supplier<SE> inverted
        boolean literal
        String noScalarValue
        Double scalar
        Term[] terms

        SE(boolean determined, String operation, String leftPart, String rightPart) {
            this.value = leftPart + " " + operation + " " + rightPart
            this.determined = determined
            this.operation = operation
            this.leftPart = leftPart
            this.rightPart = rightPart
            this.literal = false;
        }

        SE(String value, boolean determined, String operation, boolean literal) {
            this.value = value
            this.determined = determined
            this.operation = operation
            this.literal = literal
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (o == null || getClass() != o.class) return false

            SE se = (SE) o

            if (determined != se.determined) return false
            if (operation != se.operation) return false
            if (value != se.value) return false

            return true
        }

        int hashCode() {
            int result
            result = value.hashCode()
            result = 31 * result + (determined ? 1 : 0)
            result = 31 * result + (operation != null ? operation.hashCode() : 0)
            return result
        }
    }

    static class Node {
        IBoundNode boundNode
        SE v
        Supplier<SE> supplier

        Node(IBoundNode boundNode, SE v) {
            this.boundNode = boundNode
            this.v = v
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (o == null || getClass() != o.class) return false
            var node = (Node) o
            return v == node.v
        }

        int hashCode() {
            return v != null ? v.hashCode() : 0
        }
    }

    static class Formula {
        String value
        String formula

        Formula(String value, String formula) {
            this.value = value
            this.formula = formula
        }

        String getValue() {
            return value
        }

        String getFormula() {
            return formula
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (o == null || getClass() != o.class) return false

            var that = (Formula) o

            if (formula != that.formula) return false

            return true
        }

        int hashCode() {
            return formula != null ? formula.hashCode() : 0
        }
    }

}

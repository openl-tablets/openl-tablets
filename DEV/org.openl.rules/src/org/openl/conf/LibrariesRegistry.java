package org.openl.conf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.rules.operator.Operators;
import org.openl.rules.operator.CastOperators;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.rules.operator.Comparison;
import org.openl.rules.annotations.Operator;
import org.openl.rules.dt.algorithm.evaluator.CtrUtils;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.RulesUtils;
import org.openl.rules.helpers.StringRange;
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
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.CollectionUtils;

/**
 * @author Yury Molchan
 */
public class LibrariesRegistry {

    private static final LibrariesRegistry DEFAULT = new LibrariesRegistry();
    static {
        DEFAULT.addJavalib(Round.class);
        DEFAULT.addJavalib(Booleans.class);
        DEFAULT.addJavalib(Strings.class);
        DEFAULT.addJavalib(Dates.class);
        DEFAULT.addJavalib(org.openl.rules.util.Arrays.class);
        DEFAULT.addJavalib(Statistics.class);
        DEFAULT.addJavalib(Sum.class);
        DEFAULT.addJavalib(Product.class);
        DEFAULT.addJavalib(Avg.class);
        DEFAULT.addJavalib(Miscs.class);
        DEFAULT.addJavalib(Numbers.class);
        DEFAULT.addJavalib(RulesUtils.class);
        DEFAULT.addJavalib(CtrUtils.class);

        DEFAULT.addJavalib(Operators.class);
        DEFAULT.addJavalib(Comparison.class);
        DEFAULT.addJavalib(CastOperators.class);

        DEFAULT.addJavalib(IntRange.class);
        DEFAULT.addJavalib(DoubleRange.class);
        DEFAULT.addJavalib(CharRange.class);
        DEFAULT.addJavalib(StringRange.class);
        DEFAULT.addJavalib(DateRange.class);
    }

    private final HashMap<String, List<IOpenMethod>> utils = new HashMap<>();
    private final HashMap<String, List<IOpenMethod>> operators = new HashMap<>();
    private final HashMap<String, List<IOpenField>> constants = new HashMap<>();

    public void addJavalib(Class<?> cls) {
        var isOperator = cls.isAnnotationPresent(Operator.class);
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(cls);
        for (var method : cls.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                var openMethod = new JavaOpenMethod(method);
                if (isOperator || method.isAnnotationPresent(Operator.class)) {
                    operators.computeIfAbsent(method.getName(), x -> new ArrayList<>()).add(openMethod);
                } else {
                    utils.computeIfAbsent(method.getName(), x -> new ArrayList<>()).add(openMethod);
                }
            }
        }
        for (var field : openClass.getDeclaredFields()) {
            if (field.isStatic()) {
                constants.computeIfAbsent(field.getName(), x -> new ArrayList<>()).add(field);
            }
        }
    }


    public IMethodCaller getMethodCaller(String name,
                                         IOpenClass[] params,
                                         ICastFactory casts,
                                         boolean isOperator) throws AmbiguousMethodException {

        var methods = getMethods(name, isOperator);
        return MethodSearch.findMethod(name, params, casts, methods, !isOperator);
    }

    public IOpenField getField(String name) throws AmbiguousFieldException {
        if (name.equals("class")) {
            return null;
        }
        var fields = constants.get(name);
        if (fields == null || fields.isEmpty()) {
            fields = DEFAULT.constants.get(name);
        }
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        if (fields.size() == 1) {
            return fields.getFirst();
        }
        throw new AmbiguousFieldException(name, fields);
    }

    public INameSpacedMethodFactory asMethodFactory2() {
        return (namespace, name, params, casts) -> {
            boolean isOperator = ISyntaxConstants.OPERATORS_NAMESPACE.equals(namespace);
            return LibrariesRegistry.this.getMethodCaller(name, params, casts, isOperator);
        };
    }

    public INameSpacedVarFactory asVarFactory() {
        return (namespace, name, strictMatch) -> getField(name);
    }

    public IMethodFactory asMethodFactory() {
        return new IMethodFactory() {
            @Override
            public IOpenMethod getMethod(String name, IOpenClass[] params) throws AmbiguousMethodException {
                var candidates = CollectionUtils.findAll(methods(name), method -> Arrays.equals(method.getSignature().getParameterTypes(), params));
                if (candidates == null || candidates.isEmpty()) {
                    return null;
                } else if (candidates.size() == 1) {
                    return candidates.getFirst();
                }
                throw new AmbiguousMethodException(name, params, candidates);
            }

            @Override
            public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
                throw new UnsupportedOperationException("Should be never called");
            }

            @Override
            public Iterable<IOpenMethod> methods(String name) {
                return getMethods(name, true);
            }

            @Override
            public Iterable<IOpenMethod> constructors() {
                throw new UnsupportedOperationException("Should be never called");
            }
        };
    }

    private List<IOpenMethod> getMethods(String name, boolean isOperator) {
        var methods = isOperator ? operators.get(name) : utils.get(name);
        var defaultMethods = isOperator ? DEFAULT.operators.get(name) : DEFAULT.utils.get(name);

        if (defaultMethods != null) {
            if (methods == null) {
                // exists in defaults only, so just replace
                methods = defaultMethods;
            } else {
                // exists in both, so do override
                HashMap<MethodKey, ArrayList<IOpenMethod>> uniques = new HashMap<>();
                for (var method : defaultMethods) {
                    // the equal methods are collected to detect ambiguous definition from the different classes
                    uniques.computeIfAbsent(new MethodKey(method), k -> new ArrayList<>()).add(method);
                }

                HashMap<MethodKey, ArrayList<IOpenMethod>> overrides = new HashMap<>();
                for (var method : methods) {
                    // the equal methods are collected to detect ambiguous definition from the different classes
                    overrides.computeIfAbsent(new MethodKey(method), k -> new ArrayList<>()).add(method);
                }

                // Overriding of the parent methods if they exist.
                uniques.putAll(overrides);

                methods = new ArrayList<>();
                for (var m : uniques.values()) {
                    methods.addAll(m);
                }
            }
        }

        return methods;
    }
}

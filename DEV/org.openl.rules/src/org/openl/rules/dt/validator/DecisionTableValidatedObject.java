/**
 * Created Feb 8, 2007
 */
package org.openl.rules.dt.validator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.exception.OpenLRuntimeException;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.IntBoolVar;
import org.openl.ie.constrainer.IntExp;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.EnumDomainAdaptor;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.type.domains.IntRangeDomainAdaptor;
import org.openl.rules.dt.type.domains.JavaEnumDomainAdaptor;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.java.JavaEnumDomain;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 */
@RequiredArgsConstructor
@Slf4j
public class DecisionTableValidatedObject implements IDecisionTableValidatedObject, IConditionTransformer {


    @Getter
    private final IDecisionTable decisionTable;
    private Map<String, IDomainAdaptor> domainMap;
    /** The list of numbers the input of each condition takes, empty when it takes any number. */
    private final Map<IBaseCondition, Optional<EnumDomainAdaptor>> listsOfNumbers = new HashMap<>();

    public DecisionTableValidatedObject(IDecisionTable decisionTable, Map<String, IDomainAdaptor> domainMap) {
        this.decisionTable = decisionTable;
        this.domainMap = domainMap;
    }

    public synchronized Map<String, IDomainAdaptor> getDomains() {
        if (domainMap == null) {
            domainMap = makeDomains();
        }
        return domainMap;
    }

    @Override
    public IConditionTransformer getTransformer() {
        return this;
    }

    private IDomainAdaptor makeDomainAdaptor(IDomain<?> domain) {
        IDomainAdaptor result = null;
        switch (domain) {
            case EnumDomain<?> enumDomain1 -> result = new EnumDomainAdaptor(enumDomain1);
            case IntRangeDomain irange -> result = new IntRangeDomainAdaptor(irange);
            case JavaEnumDomain enumDomain -> result = new JavaEnumDomainAdaptor(enumDomain);
            case null, default -> { /* other domains have no adaptor */ }
        }
        return result;
    }

    private Map<String, IDomainAdaptor> makeDomains() {
        return new HashMap<>();
    }

    @Override
    public IntExp makeSignatureVar(String parameterName, IOpenClass paramType, Constrainer constrainer) {
        var domain = domainAdaptorOf(parameterName, paramType);
        if (domain == null && paramType.getDomain() == null
                && (boolean.class == paramType.getInstanceClass() || Boolean.class == paramType.getInstanceClass())) {
            return constrainer.addIntBoolVar(parameterName);
        }

        if (domain != null) {
            return constrainer.addIntVar(domain.getMin(), domain.getMax(), parameterName);
        }
        log.warn("Parameter '{}' has no domain.", parameterName);

        return null;

    }

    @Override
    public IOpenClass transformParameterType(IParameterDeclaration parameterDeclaration) {

        Class<?> instanceClass = parameterDeclaration.getType().getInstanceClass();

        if (String.class == instanceClass || Date.class == instanceClass) {
            return JavaOpenClass.INT;
        }

        if (IntRange.class == instanceClass) {
            return JavaOpenClass.getOpenClass(CtrIntRange.class);
        }

        if (boolean.class == instanceClass || Boolean.class == instanceClass) {
            return JavaOpenClass.INT;
        }

        if (instanceClass.isEnum()) {
            return JavaOpenClass.INT;
        }

        if (instanceClass.isArray()) {
            return JavaOpenClass.getOpenClass(int[].class);
        }

        return null;
    }

    @Override
    public Object transformLocalParameterValue(String name,
                                               IBaseCondition condition,
                                               Object value,
                                               DecisionTableAnalyzer dtan) {

        if (value != null && value.getClass().isArray()) {
            int[] res = new int[Array.getLength(value)];
            for (var i = 0; i < res.length; i++) {
                res[i] = (Integer) transformSingleLocalParameterValue(name, condition, Array.get(value, i), dtan);
            }

            return res;
        } else {
            return transformSingleLocalParameterValue(name, condition, value, dtan);
        }
    }

    public Object transformSingleLocalParameterValue(String name,
                                                     IBaseCondition condition,
                                                     Object value,
                                                     DecisionTableAnalyzer dtan) {

        var result = value;
        if (value instanceof IntRange intr) {
            var numbers = listOfNumbersOf(condition, dtan);
            return numbers == null ? new CtrIntRange(intr.getMin(), intr.getMax()) : indexesWithin(numbers, intr);
        }

        // at first search domains in those that were defined by user.
        String uniquePname = DecisionTableValidator.getUniqueConditionParamName(condition, name);
        var domainAdaptor = getDomains().get(uniquePname);
        if (domainAdaptor == null) {
            domainAdaptor = getDomains().get(name);
        }

        if (domainAdaptor != null) {
            result = domainAdaptor.getIndex(value);
        } else { // then search domains from its type.
            IDomain<?> domain = dtan.getParameterDomain(name, condition);
            if (domain != null) {
                var domainAdapt = makeDomainAdaptor(domain);
                result = domainAdapt.getIndex(value);
            } else {
                if (!(value instanceof Integer)) { // integer don`t need to be converted. so the original value
                    // will be returned. in other cases throws an exception.
                    throw new OpenLRuntimeException("Could not create domain for %s".formatted(name));
                }
            }
        }
        return result;
    }

    /**
     * The numbers of a list the input of the condition takes, such as the values of an alias datatype of whole numbers,
     * or {@code null} when the input takes any number. The input is then checked by the indexes of the numbers.
     *
     * <p>Only a condition that tests the input itself, not an expression of it such as {@code x + 1}, is checked by
     * the indexes.
     */
    private @Nullable EnumDomainAdaptor listOfNumbersOf(IBaseCondition condition, DecisionTableAnalyzer dtan) {
        return listsOfNumbers.computeIfAbsent(condition, c -> {
            var inputs = dtan.referencedSignatureParams(c);
            var adaptor = inputs.length == 1 && c.getEvaluator() instanceof ParameterMethodCaller
                    ? domainAdaptorOf(inputs[0].getName(), inputs[0].getType())
                    : null;
            return Optional.ofNullable(
                    adaptor instanceof EnumDomainAdaptor numbers && numbers.isNumeric() ? numbers : null);
        }).orElse(null);
    }

    /** The domain of a parameter: the one defined for its name, or the one of its type. */
    private @Nullable IDomainAdaptor domainAdaptorOf(String name, IOpenClass type) {
        var adaptor = getDomains().get(name);
        return adaptor == null && type.getDomain() != null ? makeDomainAdaptor(type.getDomain()) : adaptor;
    }

    /**
     * The indexes of the numbers of the list within the range. The numbers are in ascending order, so their indexes are
     * consecutive. A range holding none of them covers an index past the list, which no input takes.
     */
    private static CtrIntRange indexesWithin(EnumDomainAdaptor numbers, IntRange range) {
        var min = BigDecimal.valueOf(range.getMin());
        var max = BigDecimal.valueOf(range.getMax());
        var first = -1;
        var last = -1;
        for (var i = 0; i < numbers.size(); i++) {
            var number = EnumDomainAdaptor.decimalOf(numbers.getValue(i));
            if (number.compareTo(min) >= 0 && number.compareTo(max) <= 0) {
                first = first < 0 ? i : first;
                last = i;
            }
        }
        return first < 0 ? new CtrIntRange(numbers.size(), numbers.size()) : new CtrIntRange(first, last);
    }

    @Override
    public IOpenClass transformSignatureType(IParameterDeclaration parameterDeclaration) {

        Class<?> instanceClass = parameterDeclaration.getType().getInstanceClass();
        if (instanceClass == String.class || instanceClass == Date.class) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        if (instanceClass == int.class || instanceClass == Integer.class) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        if (instanceClass == boolean.class || instanceClass == Boolean.class) {
            return JavaOpenClass.getOpenClass(IntBoolVar.class);
        }

        if (instanceClass.isEnum()) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        return null;
    }

    @Override
    public Object transformSignatureValueBack(String name, int intValue, DecisionTableAnalyzer dtAnalyzer) {
        Object result;

        var pd = dtAnalyzer.getUsedParams().get(name);

        Class<?> instanceClass = pd.getParameterDeclaration().getType().getInstanceClass();

        if (boolean.class == instanceClass || Boolean.class == instanceClass) {
            return intValue == 1 ? "true" : "false";
        }

        // at first search domains in those that were defined by user.
        var domainAdapt = getDomains().get(name);
        if (domainAdapt != null) {
            result = domainAdapt.getValue(intValue);
        } else { // then search domains from its type.
            var domain = dtAnalyzer.getSignatureParameterDomain(name);
            if (domain != null) {
                var domainAdaptor = makeDomainAdaptor(domain);
                result = domainAdaptor.getValue(intValue);
            } else {
                result = intValue;
            }
        }

        return result;
    }

    @Override
    public boolean isOverrideAscending() {
        // 1. if return type is void, return false
        return !JavaOpenClass.VOID.equals(decisionTable.getMethod().getType());
    }

}

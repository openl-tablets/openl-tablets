package org.openl.rules.validation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public class DimensionalPropertyValidator implements IOpenLValidator {
    enum OverlapState {
        OVERLAP,
        INCLUDE_TO_A,
        INCLUDE_TO_B,
        NOT_OVERLAP,
        UNKNOWN
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        var messages = new LinkedHashSet<OpenLMessage>();
        String[] vResult = new String[3]; // 0 - INCLUDE_TO_A, 1 - INCLUDE_TO_B, 2 - OVERLAP
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher openMethodDispatcher) {
                var methods = openMethodDispatcher.getCandidates().toArray(IOpenMethod.EMPTY_ARRAY);
                for (var i = 0; i < methods.length - 1; i++) {
                    ITableProperties propsA = PropertiesHelper.getTableProperties(methods[i]);
                    Map<String, Object> propertiesA = propsA.getAllDimensionalProperties();
                    for (var j = i + 1; j < methods.length; j++) {
                        validatePair(messages, vResult, methods[i], propertiesA, methods[j]);
                    }
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    /**
     * Warns about both methods when their dimensional properties overlap.
     */
    private void validatePair(Collection<OpenLMessage> messages,
                              String[] vResult,
                              IOpenMethod methodA,
                              Map<String, Object> propertiesA,
                              IOpenMethod methodB) {
        var overlapState = OverlapState.UNKNOWN;
        for (var q = 0; q < 3; q++) {
            vResult[q] = null;
        }
        ITableProperties propsB = PropertiesHelper.getTableProperties(methodB);
        Map<String, Object> propertiesB = propsB.getAllDimensionalProperties();

        var usedKeys = new HashSet<String>(); // Performance
        // improvement
        for (var entry : propertiesA.entrySet()) {
            if (OverlapState.NOT_OVERLAP == overlapState) {
                break;
            }
            var propKey = entry.getKey();
            usedKeys.add(propKey);
            var prop = entry.getValue();
            var p = propertiesB.get(propKey);
            overlapState = loopInternal(overlapState, vResult, propKey, prop, p);
        }
        for (var entry : propertiesB.entrySet()) {
            if (OverlapState.NOT_OVERLAP == overlapState) {
                break;
            }
            var propKey = entry.getKey();
            if (!usedKeys.contains(propKey)) {
                var prop = propertiesA.get(propKey);
                var p = entry.getValue();
                overlapState = loopInternal(overlapState, vResult, propKey, prop, p);
            }
        }

        if (overlapState == OverlapState.OVERLAP) {
            var sb = new StringBuilder();
            writeOverlapMessage(sb, vResult, propertiesA, propertiesB);
            addValidationWarn(messages, sb.toString(), methodA);
            addValidationWarn(messages, sb.toString(), methodB);
        }
    }

    /**
     * Writes the values of the overlapping property, or the values of both properties one of the methods includes.
     */
    private void writeOverlapMessage(StringBuilder sb,
                                     String[] vResult,
                                     Map<String, Object> propertiesA,
                                     Map<String, Object> propertiesB) {
        if (vResult[2] != null) {
            var pKey = vResult[2];
            var valueA = propertiesA.get(pKey);
            var valueB = propertiesB.get(pKey);
            sb.append("(");
            writeMessageForProperty(sb, pKey, valueA);
            sb.append(")");
            sb.append(" and ");
            sb.append("(");
            writeMessageForProperty(sb, pKey, valueB);
            sb.append(")");
        } else {
            var pKey1 = vResult[0];
            var value1A = propertiesA.get(pKey1);
            var value1B = propertiesB.get(pKey1);
            var pKey2 = vResult[1];
            var value2A = propertiesA.get(pKey2);
            var value2B = propertiesB.get(pKey2);
            sb.append("(");
            writeMessageForProperty(sb, pKey1, value1A);
            sb.append(", ");
            writeMessageForProperty(sb, pKey2, value2A);
            sb.append(")");
            sb.append(" and ");
            sb.append("(");
            writeMessageForProperty(sb, pKey1, value1B);
            sb.append(", ");
            writeMessageForProperty(sb, pKey2, value2B);
            sb.append(")");
        }
    }

    OverlapState loopInternal(OverlapState overlapState, String[] vResult, String propKey, Object prop, Object p) {
        if (prop == null && p == null) { // Go to next
            return overlapState;
        }
        if (!OverlapState.OVERLAP.equals(overlapState)) {
            if (prop == null) {
                return includeToA(overlapState, vResult, propKey);
            }
            if (p == null) {
                return includeToB(overlapState, vResult, propKey);
            }
        } else {
            if (prop == null || p == null) { // Go to next
                return overlapState;
            }
        }
        if (prop.getClass().isArray()) {
            return compareArrays(overlapState, vResult, propKey, prop, p);
        }
        if (!prop.equals(p)) {
            overlapState = OverlapState.NOT_OVERLAP; // Skip
            // other
            // properties
        }
        return overlapState;
    }

    /**
     * Compares the elements of two array values: no common element means no overlap, and the values of one array
     * all being in the other means inclusion.
     */
    private OverlapState compareArrays(OverlapState overlapState,
                                       String[] vResult,
                                       String propKey,
                                       Object prop,
                                       Object p) {
        var length1 = Array.getLength(prop);
        var length2 = Array.getLength(p);
        var f1 = false;
        var f2 = false;

        var propSet = arrayToSet(prop, length1);
        var pSet = arrayToSet(p, length2);

        propSet.retainAll(pSet);
        var d = propSet.size();

        if (OverlapState.OVERLAP != overlapState) {
            if (length1 < length2) {
                f2 = d == length1;
            } else {
                f1 = d == length2;
                if (length1 == length2) {
                    f2 = f1;
                }
            }
        }

        var f3 = d == 0;

        if (f3) {
            overlapState = OverlapState.NOT_OVERLAP;
            return overlapState;
        }
        if (OverlapState.OVERLAP != overlapState) {
            return includeByArrays(overlapState, vResult, propKey, f1, f2);
        }
        return overlapState;
    }

    /**
     * Updates the state by which of the arrays includes the other one. Equal arrays keep the state, and arrays that do
     * not include each other overlap.
     */
    private static OverlapState includeByArrays(OverlapState overlapState,
                                                String[] vResult,
                                                String propKey,
                                                boolean f1,
                                                boolean f2) {
        if (f1 && f2) {
            return overlapState;
        }
        if (f1) {
            return includeToA(overlapState, vResult, propKey);
        }
        if (f2) {
            return includeToB(overlapState, vResult, propKey);
        }
        vResult[2] = propKey;
        return OverlapState.OVERLAP;
    }

    private static OverlapState includeToA(OverlapState overlapState, String[] vResult, String propKey) {
        OverlapState state;
        if (OverlapState.INCLUDE_TO_B == overlapState) {
            state = OverlapState.OVERLAP;
        } else {
            state = OverlapState.INCLUDE_TO_A;
        }
        vResult[0] = propKey;
        return state;
    }

    private static OverlapState includeToB(OverlapState overlapState, String[] vResult, String propKey) {
        OverlapState state;
        if (OverlapState.INCLUDE_TO_A == overlapState) {
            state = OverlapState.OVERLAP;
        } else {
            state = OverlapState.INCLUDE_TO_B;
        }
        vResult[1] = propKey;
        return state;
    }

    private Set<Object> arrayToSet(Object array, int length) {
        var set = new HashSet<Object>();
        for (var k = 0; k < length; k++) {
            set.add(Array.get(array, k));
        }
        return set;
    }

    private void writeMessageForProperty(StringBuilder sb, String pKey, Object value) {
        sb.append(pKey);
        sb.append("={");
        if (value != null) {
            if (value.getClass().isArray()) {
                var length = Array.getLength(value);
                for (var k = 0; k < length; k++) {
                    if (k != 0) {
                        sb.append(", ");
                    }
                    writeObject(sb, Array.get(value, k));
                }
            } else {
                writeObject(sb, value);
            }
        }
        sb.append("}");
    }

    private void writeObject(StringBuilder sb, Object value) {
        if (value.getClass().isEnum()) {
            sb.append(((Enum<?>) value).name());
        } else {
            sb.append(value.toString());
        }
    }

    private void addValidationWarn(Collection<OpenLMessage> messages, String message, IOpenMethod method) {
        var memberMetaInfo = (IMemberMetaInfo) method;
        if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
            messages
                    .add(OpenLMessagesUtils.newWarnMessage("Ambiguous definition of properties values. Details: " + message,
                            memberMetaInfo.getSyntaxNode()));
        }
    }

}

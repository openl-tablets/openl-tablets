package org.openl.rules.validation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public class DimentionalPropertyValidator implements IOpenLValidator {
    private enum OverlapState {
        OVERLAP,
        INCLUDE_TO_A,
        INCLUDE_TO_B,
        NOT_OVERLAP,
        UNKNOWN
    }

    @Override
    public ValidationResult validate(OpenL openl, IOpenClass openClass) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                IOpenMethod[] methods = openMethodDispatcher.getCandidates().toArray(new IOpenMethod[] {});
                for (int i = 0; i < methods.length - 1; i++) {
                    ITableProperties propsA = PropertiesHelper.getTableProperties(methods[i]);
                    Map<String, Object> propertiesA = propsA.getAllDimensionalProperties();
                    for (int j = i + 1; j < methods.length; j++) {
                        OverlapState overlapState = OverlapState.UNKNOWN;
                        Map<OverlapState, String> vResult = new HashMap<DimentionalPropertyValidator.OverlapState, String>();
                        ITableProperties propsB = PropertiesHelper.getTableProperties(methods[j]);
                        Map<String, Object> propertiesB = propsB.getAllDimensionalProperties();

                        Set<String> usedKeys = new HashSet<String>(); //Performance improvement
                        for (String propKey : propertiesA.keySet()) {
                            if (OverlapState.NOT_OVERLAP == overlapState) {
                                break;
                            }
                            usedKeys.add(propKey);
                            Object prop = propertiesA.get(propKey);
                            Object p = propertiesB.get(propKey);
                            overlapState = loopInternal(overlapState, vResult, propKey, prop, p);
                        }
                        for (String propKey : propertiesB.keySet()) {
                            if (OverlapState.NOT_OVERLAP == overlapState) {
                                break;
                            }
                            if (usedKeys.contains(propKey)) {
                                continue;
                            }
                            Object prop = propertiesA.get(propKey);
                            Object p = propertiesB.get(propKey);
                            overlapState = loopInternal(overlapState, vResult, propKey, prop, p);
                        }

                        if (overlapState == OverlapState.OVERLAP) {
                            StringBuilder sb = new StringBuilder();
                            if (vResult.containsKey(OverlapState.OVERLAP)) {
                                String pKey = vResult.get(OverlapState.OVERLAP);
                                Object valueA = propertiesA.get(pKey);
                                Object valueB = propertiesB.get(pKey);
                                sb.append("(");
                                writeMessageforProperty(sb, pKey, valueA);
                                sb.append(")");
                                sb.append(" and ");
                                sb.append("(");
                                writeMessageforProperty(sb, pKey, valueB);
                                sb.append(")");
                            } else {
                                String pKey1 = vResult.get(OverlapState.INCLUDE_TO_A);
                                Object value1A = propertiesA.get(pKey1);
                                Object value1B = propertiesB.get(pKey1);
                                String pKey2 = vResult.get(OverlapState.INCLUDE_TO_B);
                                Object value2A = propertiesA.get(pKey2);
                                Object value2B = propertiesB.get(pKey2);
                                sb.append("(");
                                writeMessageforProperty(sb, pKey1, value1A);
                                sb.append(", ");
                                writeMessageforProperty(sb, pKey2, value2A);
                                sb.append(")");
                                sb.append(" and ");
                                sb.append("(");
                                writeMessageforProperty(sb, pKey1, value1B);
                                sb.append(", ");
                                writeMessageforProperty(sb, pKey2, value2B);
                                sb.append(")");
                            }
                            addValidationWarn(messages, sb.toString(), methods[i]);
                            addValidationWarn(messages, sb.toString(), methods[j]);
                        }
                    }
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private OverlapState loopInternal(OverlapState overlapState,
            Map<OverlapState, String> vResult,
            String propKey,
            Object prop,
            Object p) {
        if (prop == null && p == null) { // Go to next
            return overlapState;
        }
        if (!OverlapState.OVERLAP.equals(overlapState)) {
            if (prop == null) {
                if (OverlapState.INCLUDE_TO_B.equals(overlapState)) {
                    overlapState = OverlapState.OVERLAP;
                } else {
                    overlapState = OverlapState.INCLUDE_TO_A;
                }
                vResult.put(OverlapState.INCLUDE_TO_A, propKey);
                return overlapState;
            }
            if (p == null) {
                if (OverlapState.INCLUDE_TO_A.equals(overlapState)) {
                    overlapState = OverlapState.OVERLAP;
                } else {
                    overlapState = OverlapState.INCLUDE_TO_B;
                }
                vResult.put(OverlapState.INCLUDE_TO_B, propKey);
                return overlapState;
            }
        } else {
            if (prop == null || p == null) { // Go to next
                return overlapState;
            }
        }
        if (prop.getClass().isArray()) {
            int length1 = Array.getLength(prop);
            int length2 = Array.getLength(p);
            boolean f1 = false;
            boolean f2 = false;
            int d = 0;
            if (!OverlapState.OVERLAP.equals(overlapState)) {
                if (length1 < length2) {
                    for (int k = 0; k < length2; k++) {
                        for (int q = 0; q < length1; q++) {
                            if (Array.get(p, k).equals(Array.get(prop, q))) {
                                d++;
                            }
                        }
                    }
                    f2 = (d == length1);
                } else {
                    for (int k = 0; k < length1; k++) {
                        for (int q = 0; q < length2; q++) {
                            if (Array.get(prop, k).equals(Array.get(p, q))) {
                                d++;
                            }
                        }
                    }
                    f1 = (d == length2);
                    if (length1 == length2) {
                        f2 = f1;
                    }
                } 
            } else {
                for (int k = 0; k < length1; k++) {
                    for (int q = 0; q < length2; q++) {
                        if (Array.get(prop, k).equals(Array.get(p, q))) {
                            d++;
                        }
                    }
                }
            }
            
            boolean f3 = d == 0;
            
            if (f3) {
                overlapState = OverlapState.NOT_OVERLAP;
                return overlapState;
            }
            if (!OverlapState.OVERLAP.equals(overlapState)) {
                if (f1 && f2) {
                    return overlapState;
                }
                if (f1) {
                    if (OverlapState.INCLUDE_TO_B.equals(overlapState)) {
                        overlapState = OverlapState.OVERLAP;
                    } else {
                        overlapState = OverlapState.INCLUDE_TO_A;
                    }
                    vResult.put(OverlapState.INCLUDE_TO_A, propKey);
                    return overlapState;
                }
                if (f2) {
                    if (OverlapState.INCLUDE_TO_A.equals(overlapState)) {
                        overlapState = OverlapState.OVERLAP;
                    } else {
                        overlapState = OverlapState.INCLUDE_TO_B;
                    }
                    vResult.put(OverlapState.INCLUDE_TO_B, propKey);
                    return overlapState;
                }
                overlapState = OverlapState.OVERLAP;
                vResult.put(OverlapState.OVERLAP, propKey);
            }
        } else {
            if (prop.equals(p)) {
                return overlapState;
            } else {
                overlapState = OverlapState.NOT_OVERLAP; // Skip
                                                         // other
                                                         // properties
                return overlapState;
            }
        }
        return overlapState;
    }

    private void writeMessageforProperty(StringBuilder sb, String pKey, Object value) {
        sb.append(pKey);
        sb.append("={");
        if (value != null) {
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                for (int k = 0; k < length; k++) {
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
        IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) method;
        if (memberMetaInfo.getSyntaxNode() != null) {
            if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
                messages.add(
                    OpenLMessagesUtils.newWarnMessage("Ambiguous definition of properties values. Details: " + message,
                        memberMetaInfo.getSyntaxNode()));
            }
        }
    }

}

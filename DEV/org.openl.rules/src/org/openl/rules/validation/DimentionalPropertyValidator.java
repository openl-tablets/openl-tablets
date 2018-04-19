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
        Set<String> dimensionalProperties = getDimensionalProperties();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                IOpenMethod[] methods = openMethodDispatcher.getCandidates().toArray(new IOpenMethod[] {});
                for (int i = 0; i < methods.length - 1; i++) {
                    ITableProperties propsA = PropertiesHelper.getTableProperties(methods[i]);
                    Map<String, Object> propertiesA = propsA.getAllProperties();
                    for (int j = i + 1; j < methods.length; j++) {
                        OverlapState overlapState = OverlapState.UNKNOWN;
                        Map<OverlapState, String> vResult = new HashMap<DimentionalPropertyValidator.OverlapState, String>();
                        ITableProperties propsB = PropertiesHelper.getTableProperties(methods[j]);
                        Map<String, Object> propertiesB = propsB.getAllProperties();

                        Set<String> propKeys = new HashSet<String>();
                        propKeys.addAll(propertiesA.keySet());
                        propKeys.addAll(propertiesB.keySet());

                        for (String propKey : propKeys) {
                            if (!dimensionalProperties.contains(propKey)) { // Skip
                                                                            // not
                                                                            // dimansionalProperties
                                continue;
                            }
                            Object prop = propertiesA.get(propKey);
                            Object p = propertiesB.get(propKey);
                            if (prop == null && p == null) { // Go to next
                                                             // property
                                continue;
                            }
                            if (!OverlapState.OVERLAP.equals(overlapState)) {
                                if (prop == null) {
                                    if (OverlapState.INCLUDE_TO_B.equals(overlapState)) {
                                        overlapState = OverlapState.OVERLAP;
                                    } else {
                                        overlapState = OverlapState.INCLUDE_TO_A;
                                    }
                                    vResult.put(OverlapState.INCLUDE_TO_A, propKey);
                                    continue; // Go to next property
                                }
                                if (p == null) {
                                    if (OverlapState.INCLUDE_TO_A.equals(overlapState)) {
                                        overlapState = OverlapState.OVERLAP;
                                    } else {
                                        overlapState = OverlapState.INCLUDE_TO_B;
                                    }
                                    vResult.put(OverlapState.INCLUDE_TO_B, propKey);
                                    continue; // Go to next property
                                }
                            } else {
                                if (prop == null || p == null) { // Go to next
                                                                 // property
                                    continue;
                                }
                            }
                            if (prop.getClass().isArray()) {
                                Set<Object> propSet = new HashSet<Object>();
                                int length = Array.getLength(prop);
                                for (int k = 0; k < length; k++) {
                                    propSet.add(Array.get(prop, k));
                                }
                                Set<Object> pSet = new HashSet<Object>();
                                length = Array.getLength(p);
                                for (int k = 0; k < length; k++) {
                                    pSet.add(Array.get(p, k));
                                }
                                boolean f1 = false;
                                boolean f2 = false;
                                if (!OverlapState.OVERLAP.equals(overlapState)) {
                                    f1 = propSet.containsAll(pSet);
                                    f2 = pSet.containsAll(propSet);
                                }
                                propSet.retainAll(pSet);
                                boolean f3 = propSet.isEmpty();
                                if (f3) {
                                    overlapState = OverlapState.NOT_OVERLAP;
                                    break;
                                }
                                if (!OverlapState.OVERLAP.equals(overlapState)) {
                                    if (f1 && f2) {
                                        continue;
                                    }
                                    if (f1) {
                                        if (OverlapState.INCLUDE_TO_B.equals(overlapState)) {
                                            overlapState = OverlapState.OVERLAP;
                                        } else {
                                            overlapState = OverlapState.INCLUDE_TO_A;
                                        }
                                        vResult.put(OverlapState.INCLUDE_TO_A, propKey);
                                        continue;
                                    }
                                    if (f2) {
                                        if (OverlapState.INCLUDE_TO_A.equals(overlapState)) {
                                            overlapState = OverlapState.OVERLAP;
                                        } else {
                                            overlapState = OverlapState.INCLUDE_TO_B;
                                        }
                                        vResult.put(OverlapState.INCLUDE_TO_B, propKey);
                                        continue;
                                    }
                                    overlapState = OverlapState.OVERLAP;
                                    vResult.put(OverlapState.OVERLAP, propKey);
                                }
                            } else {
                                if (prop.equals(p)) {
                                    continue; // Go to next property
                                } else {
                                    overlapState = OverlapState.NOT_OVERLAP; // Skip
                                                                             // other
                                                                             // properties
                                    break;
                                }
                            }
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

    private Set<String> getDimensionalProperties() {
        String[] dPropertiesArray = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        Set<String> dProperties = new HashSet<String>();
        for (String d : dPropertiesArray) {
            dProperties.add(d);
        }
        return dProperties;
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

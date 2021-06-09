package org.openl.rules.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public class UniqueMethodParameterNamesValidator implements IOpenLValidator {

    private static final String MSG_FOR_TYPES = "Method '%s' conflicts with another method '%s', because of parameter types are different.";
    private static final String MSG_FOR_NAMES = "Method '%s' conflicts with another method '%s', because of parameter names are different.";

    private interface ParameterKey {
        IOpenMethod getMethod();
    }

    private static class ParameterNameKey implements ParameterKey {
        String name;
        IOpenMethod method;

        public ParameterNameKey(String name, IOpenMethod method) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.method = Objects.requireNonNull(method, "method cannot be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParameterNameKey that = (ParameterNameKey) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public IOpenMethod getMethod() {
            return method;
        }
    }

    private static class ParameterTypeKey implements ParameterKey {
        IOpenClass type;
        IOpenMethod method;

        public ParameterTypeKey(IOpenClass type, IOpenMethod method) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.method = Objects.requireNonNull(method, "method cannot be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParameterTypeKey that = (ParameterTypeKey) o;
            return type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public IOpenMethod getMethod() {
            return method;
        }
    }

    private static class MethodPairKey {
        IOpenMethod methodA;
        IOpenMethod methodB;

        public MethodPairKey(IOpenMethod methodA, IOpenMethod methodB) {
            this.methodA = Objects.requireNonNull(methodA, "methodA cannot be null");
            this.methodB = Objects.requireNonNull(methodB, "methodB cannot be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodPairKey that = (MethodPairKey) o;
            return Objects.equals(methodA, that.methodA) && Objects.equals(methodB, that.methodB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodA, methodB);
        }
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                List<IOpenMethod> candidates = openMethodDispatcher.getCandidates();
                int parameterCount = candidates.iterator().next().getSignature().getNumberOfParameters();
                Set<ParameterNameKey>[] parameterKeysByName = new HashSet[parameterCount];
                Set<ParameterTypeKey>[] parameterKeysByType = new HashSet[parameterCount];
                for (int i = 0; i < parameterCount; i++) {
                    parameterKeysByName[i] = new HashSet<>();
                    parameterKeysByType[i] = new HashSet<>();
                }
                for (IOpenMethod candidate : candidates) {
                    IMethodSignature signature = candidate.getSignature();
                    for (int j = 0; j < parameterCount; j++) {
                        parameterKeysByName[j].add(new ParameterNameKey(signature.getParameterName(j), candidate));
                        parameterKeysByType[j].add(new ParameterTypeKey(signature.getParameterType(j), candidate));
                    }
                }
                for (MethodPairKey methodPair : buildMethodPairs(parameterKeysByName, parameterCount)) {
                    addWarnForMethods(methodPair.methodA, methodPair.methodB, messages, MSG_FOR_NAMES);
                }
                for (MethodPairKey methodPair : buildMethodPairs(parameterKeysByType, parameterCount)) {
                    addWarnForMethods(methodPair.methodA, methodPair.methodB, messages, MSG_FOR_TYPES);
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private Set<MethodPairKey> buildMethodPairs(Set<? extends ParameterKey>[] parameterKeys, int parameterCount) {
        Set<MethodPairKey> methodPairs = new HashSet<>();
        for (int i = 0; i < parameterCount; i++) {
            if (parameterKeys[i].size() > 1) {
                List<IOpenMethod> conflictMethods = parameterKeys[i].stream()
                    .map(ParameterKey::getMethod)
                    .collect(Collectors.toList());
                for (int j = 0; j < conflictMethods.size() - 1; j++) {
                    for (int k = j + 1; k < conflictMethods.size(); k++) {
                        methodPairs.add(new MethodPairKey(conflictMethods.get(j), conflictMethods.get(k)));
                    }
                }
            }
        }
        return methodPairs;
    }

    private void addWarnForMethods(IOpenMethod methodA,
            IOpenMethod methodB,
            Collection<OpenLMessage> messages,
            String message) {
        ISyntaxNode syntaxNodeA = ((IMemberMetaInfo) methodA).getSyntaxNode();
        ISyntaxNode syntaxNodeB = ((IMemberMetaInfo) methodB).getSyntaxNode();
        String signA = MethodUtil.printSignature(methodA, INamedThing.REGULAR);
        String signB = MethodUtil.printSignature(methodB, INamedThing.REGULAR);
        String messageA = String.format(message, signA, signB);
        String messageB = String.format(message, signB, signA);
        if (syntaxNodeA instanceof TableSyntaxNode) {
            messages.add(OpenLMessagesUtils.newWarnMessage(messageA, syntaxNodeA));
        }
        if (syntaxNodeB instanceof TableSyntaxNode) {
            messages.add(OpenLMessagesUtils.newWarnMessage(messageB, syntaxNodeB));
        }
    }
}

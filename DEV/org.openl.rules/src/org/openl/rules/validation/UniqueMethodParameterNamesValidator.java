package org.openl.rules.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public class UniqueMethodParameterNamesValidator implements IOpenLValidator {

    private static final String MSG_FOR_NAMES = "Method '%s' conflicts with another method '%s', because of parameter names are different.";

    private interface ParameterKey {
        IOpenMethod getMethod();
    }

    private static class ParameterNameKey implements ParameterKey {
        String name;
        @Getter
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
            var that = (ParameterNameKey) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
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
            var that = (MethodPairKey) o;
            return Objects.equals(methodA, that.methodA) && Objects.equals(methodB, that.methodB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodA, methodB);
        }
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        var messages = new LinkedHashSet<OpenLMessage>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher openMethodDispatcher) {
                List<IOpenMethod> candidates = openMethodDispatcher.getCandidates();
                var parameterCount = candidates.getFirst().getSignature().getNumberOfParameters();
                Set<ParameterNameKey>[] parameterKeysByName = new HashSet[parameterCount];
                for (var i = 0; i < parameterCount; i++) {
                    parameterKeysByName[i] = new HashSet<>();
                }
                for (IOpenMethod candidate : candidates) {
                    var signature = candidate.getSignature();
                    for (var j = 0; j < parameterCount; j++) {
                        if (signature.getParameterName(j) != null) {
                            parameterKeysByName[j].add(new ParameterNameKey(signature.getParameterName(j), candidate));
                        }
                    }
                }
                for (MethodPairKey methodPair : buildMethodPairs(parameterKeysByName, parameterCount)) {
                    addWarnForMethods(methodPair.methodA, methodPair.methodB, messages, MSG_FOR_NAMES);
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private Set<MethodPairKey> buildMethodPairs(Set<? extends ParameterKey>[] parameterKeys, int parameterCount) {
        var methodPairs = new HashSet<MethodPairKey>();
        for (var i = 0; i < parameterCount; i++) {
            if (parameterKeys[i].size() > 1) {
                var conflictMethods = parameterKeys[i].stream()
                        .map(ParameterKey::getMethod)
                        .collect(Collectors.toList());
                for (var j = 0; j < conflictMethods.size() - 1; j++) {
                    for (var k = j + 1; k < conflictMethods.size(); k++) {
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
        var syntaxNodeA = ((IMemberMetaInfo) methodA).getSyntaxNode();
        var syntaxNodeB = ((IMemberMetaInfo) methodB).getSyntaxNode();
        String signA = MethodUtil.printSignature(methodA, INamedThing.REGULAR);
        String signB = MethodUtil.printSignature(methodB, INamedThing.REGULAR);
        var messageA = message.formatted(signA, signB);
        var messageB = message.formatted(signB, signA);
        if (syntaxNodeA instanceof TableSyntaxNode) {
            messages.add(OpenLMessagesUtils.newWarnMessage(messageA, syntaxNodeA));
        }
        if (syntaxNodeB instanceof TableSyntaxNode) {
            messages.add(OpenLMessagesUtils.newWarnMessage(messageB, syntaxNodeB));
        }
    }
}

package org.openl.rules.validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

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

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                for (int i = 0; i < openMethodDispatcher.getCandidates().size() - 1; i++) {
                    for (int j = i + 1; j < openMethodDispatcher.getCandidates().size(); j++) {
                        IOpenMethod methodA = openMethodDispatcher.getCandidates().get(i);
                        IOpenMethod methodB = openMethodDispatcher.getCandidates().get(j);

                        validateSignature(messages, methodA, methodB);
                    }
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private void validateSignature(Collection<OpenLMessage> messages, IOpenMethod methodA, IOpenMethod methodB) {
        IMethodSignature signatureA = methodA.getSignature();
        IMethodSignature signatureB = methodB.getSignature();
        for (int i1 = 0; i1 < signatureA.getNumberOfParameters(); i1++) {
            if (!Objects.equals(signatureA.getParameterName(i1), signatureB.getParameterName(i1))) {
                addWarnForMethods(methodA, methodB, messages, MSG_FOR_NAMES);
                break;
            }
            if (!Objects.equals(signatureA.getParameterType(i1), signatureB.getParameterType(i1))) {
                addWarnForMethods(methodA, methodB, messages, MSG_FOR_TYPES);
                break;
            }
        }
    }

    private void addWarnForMethods(IOpenMethod methodA, IOpenMethod methodB, Collection<OpenLMessage> messages,
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

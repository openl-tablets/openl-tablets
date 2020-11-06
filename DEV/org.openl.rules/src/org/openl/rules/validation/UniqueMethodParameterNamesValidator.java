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
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public class UniqueMethodParameterNamesValidator implements IOpenLValidator {
    @Override
    public ValidationResult validate(IOpenClass openClass) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                for (int i = 0; i < openMethodDispatcher.getCandidates().size() - 1; i++) {
                    for (int j = i + 1; j < openMethodDispatcher.getCandidates().size(); j++) {
                        if (isConflictOnMethodParams(openMethodDispatcher.getCandidates().get(i),
                            openMethodDispatcher.getCandidates().get(j))) {
                            addWarnToForMethod(messages,
                                openMethodDispatcher.getCandidates().get(i),
                                openMethodDispatcher.getCandidates().get(j));
                            addWarnToForMethod(messages,
                                openMethodDispatcher.getCandidates().get(j),
                                openMethodDispatcher.getCandidates().get(i));
                        }
                    }
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private void addWarnToForMethod(Collection<OpenLMessage> messages,
            IOpenMethod method,
            IOpenMethod conflictsWithMethod) {
        IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) method;
        if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
            String message = String.format(
                "Method '%s' conflicts with another method '%s', because parameter names are different.",
                MethodUtil.printSignature(method, INamedThing.REGULAR),
                MethodUtil.printSignature(conflictsWithMethod, INamedThing.REGULAR));
            messages.add(OpenLMessagesUtils.newWarnMessage(message, memberMetaInfo.getSyntaxNode()));
        }
    }

    private boolean isConflictOnMethodParams(IOpenMethod method1, IOpenMethod method2) {
        for (int i = 0; i < method1.getSignature().getNumberOfParameters(); i++) {
            if (!Objects.equals(method1.getSignature().getParameterName(i),
                method2.getSignature().getParameterName(i))) {
                return true;
            }
        }
        return false;
    }
}

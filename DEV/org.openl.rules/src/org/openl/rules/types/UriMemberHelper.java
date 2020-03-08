package org.openl.rules.types;

import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public final class UriMemberHelper {

    private UriMemberHelper() {
    }

    private static boolean isNotTheSame(IUriMember uriMember1, IUriMember uriMember2) {
        return !uriMember1.getUri().equals(uriMember2.getUri());
    }

    /**
     * Throw the error with the right message for the case when the methods are equal
     */
    public static void validateMethodDuplication(IOpenMethod method, IOpenMethod existedMethod) {
        if (method instanceof IUriMember && existedMethod instanceof IUriMember) {
            if (isNotTheSame((IUriMember) method, (IUriMember) existedMethod)) {
                String message = ValidationMessages.getDuplicatedMethodMessage(existedMethod, method);
                throw new DuplicatedMethodException(message, existedMethod, method);
            }
        } else {
            throw new IllegalStateException("Implementation supports only IUriMember.");
        }
    }

    public static void validateFieldDuplication(IOpenField openField, IOpenField existedField) {
        if (openField instanceof IUriMember && existedField instanceof IUriMember) {
            if (isNotTheSame((IUriMember) openField, (IUriMember) existedField)) {
                throw new DuplicatedFieldException("", openField.getName());
            }
        } else {
            throw new IllegalStateException("Implementation supports only IUriMember.");
        }
    }
}

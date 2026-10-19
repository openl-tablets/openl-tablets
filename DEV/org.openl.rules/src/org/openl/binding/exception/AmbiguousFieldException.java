/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

import org.openl.base.INamedThing;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 */
public class AmbiguousFieldException extends OpenlNotCheckedException {

    @Getter
    private final transient Collection<IOpenField> matchingFields;

    private final String fieldName;

    public AmbiguousFieldException(String fieldName, List<IOpenField> matchingFields) {
        this.fieldName = fieldName;
        this.matchingFields = Collections.unmodifiableList(matchingFields);
    }

    @Override
    public String getMessage() {
        var sb = new StringBuilder();

        sb.append("Field '").append(fieldName).append("' is ambiguous");
        if (matchingFields != null) {
            sb.append(":\nMatching fields:\n");
            var first = true;
            var declaringClasses = matchingFields.stream().map(IOpenField::getDeclaringClass).distinct().count();
            for (IOpenField f : matchingFields) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(declaringClasses == 1 ? "" : qualifierOf(f)).append(f.getDisplayName(INamedThing.SHORT));
                first = false;
            }
        } else {
            sb.append(".");
        }
        return sb.toString();
    }

    /**
     * Names the type that declares the field, as {@code Runner.} for {@code Runner.age}. A variable, such as the
     * element an array index operator names, belongs to no type, so it is named alone.
     */
    private static String qualifierOf(IOpenField field) {
        var declaringClass = field.getDeclaringClass();
        return declaringClass == null || NullOpenClass.isAnyNull(declaringClass) ? ""
                : declaringClass.getDisplayName(INamedThing.SHORT) + ".";
    }

}

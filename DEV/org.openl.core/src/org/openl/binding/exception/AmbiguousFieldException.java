/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.List;

import org.openl.base.INamedThing;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class AmbiguousFieldException extends OpenlNotCheckedException {

    private final List<IOpenField> matchingFields;

    private final String fieldName;

    public AmbiguousFieldException(String fieldName, List<IOpenField> matchingFields) {
        this.fieldName = fieldName;
        this.matchingFields = matchingFields;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Field ").append(fieldName);
        sb.append(" is ambiguous:\n").append("Matching fields:\n");
        boolean first = true;
        for (IOpenField f : matchingFields) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(f.getDisplayName(INamedThing.LONG));
            first = false;
        }

        return sb.toString();
    }

}

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
public class AmbiguousVarException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -8752617383143899614L;

    private List<IOpenField> matchingFields;
    
    private String varName;

    public AmbiguousVarException(String varName, List<IOpenField> matchingFields) {
        this.varName = varName;
        this.matchingFields = matchingFields;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Variable ").append(varName);
        sb.append(" is ambiguous:\n").append("Matching fieldValues:\n");
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

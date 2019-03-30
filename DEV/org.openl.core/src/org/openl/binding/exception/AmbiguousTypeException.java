/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.List;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class AmbiguousTypeException extends OpenlNotCheckedException {

    /**
     *
     */
    private static final long serialVersionUID = 3432594431020887309L;

    private List<IOpenClass> matchingTypes;
    private String typeName;

    public AmbiguousTypeException(String typeName, List<IOpenClass> matchingTypes) {
        this.typeName = typeName;
        this.matchingTypes = matchingTypes;
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();

        buf.append("Type ").append(typeName);
        buf.append(" is ambiguous:\n").append("Matching types:\n");
        for (IOpenClass type : matchingTypes) {
            buf.append(type.getName()).append('\n');
        }

        return buf.toString();
    }

}

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

    private final List<IOpenClass> matchingTypes;
    private final String typeName;

    public AmbiguousTypeException(String typeName, List<IOpenClass> matchingTypes) {
        this.typeName = typeName;
        this.matchingTypes = matchingTypes;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Type '").append(typeName);
        sb.append("' is ambiguous:\n").append("Matching types:\n");
        for (IOpenClass type : matchingTypes) {
            sb.append(type.getName()).append('\n');
        }

        return sb.toString();
    }

}

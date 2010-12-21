/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.Iterator;
import java.util.List;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class AmbiguousTypeException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 3432594431020887309L;
    List<IOpenClass> matchingTypes;
    String typeName;

    public AmbiguousTypeException(String typeName, List<IOpenClass> matchingTypes) {
        this.typeName = typeName;
        this.matchingTypes = matchingTypes;
    }

    @Override
    public String getMessage() {
        StringBuffer buf = new StringBuffer();

        buf.append("Type ").append(typeName);
        buf.append(" is ambiguous:\n").append("Matching types:\n");
        for (Iterator<IOpenClass> iter = matchingTypes.iterator(); iter.hasNext();) {
            IOpenClass type = iter.next();
            buf.append(type.getName()).append('\n');
        }

        return buf.toString();
    }

}

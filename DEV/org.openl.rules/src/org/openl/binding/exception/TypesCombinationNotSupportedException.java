package org.openl.binding.exception;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;

/**
 * If type combination is not possible then this exception is thrown.
 *
 * @author Marat Kamalov
 */
@RequiredArgsConstructor
public class TypesCombinationNotSupportedException extends OpenlNotCheckedException {

    private final List<IOpenClass> type;

    @Override
    public String getMessage() {
        var sb = new StringBuilder();

        sb.append("Combination of types are not support:\n");
        for (IOpenClass type : type) {
            sb.append(type.getName()).append('\n');
        }

        return sb.toString();
    }

}

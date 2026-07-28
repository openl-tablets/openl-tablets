package org.openl.rules.tbasic.runtime.operations;

import java.util.Iterator;

import lombok.RequiredArgsConstructor;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * Created by dl on 9/10/14.
 */
@RequiredArgsConstructor
public class IteratorNextOperation extends RuntimeOperation {

    public static final String ITERATOR = "iterator";
    private final String elementName;

    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        var iterator = (Iterator) environment.getTbasicTarget().getFieldValue(ITERATOR + elementName);
        return new Result(ReturnType.NEXT, iterator.next());
    }
}

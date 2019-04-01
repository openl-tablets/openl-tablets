package org.openl.rules.tbasic.runtime.operations;

import java.util.Iterator;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * Created by dl on 9/10/14.
 */
public class IteratorNextOperation extends RuntimeOperation {

    public static final String ITERATOR = "iterator";
    private String elementName;

    public IteratorNextOperation(String elementName) {
        this.elementName = elementName;
    }

    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        Iterator iterator = (Iterator) environment.getTbasicTarget().getFieldValue(ITERATOR + elementName);
        return new Result(ReturnType.NEXT, iterator.next());
    }
}

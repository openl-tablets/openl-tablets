package org.openl.rules.tbasic.runtime.operations;

import java.util.Arrays;
import java.util.Iterator;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * Created by dl on 9/10/14.
 */
public class DeclareIteratorOperation extends RuntimeOperation {

    private String elementName;

    public DeclareIteratorOperation(String label, String elementName) {
        this.elementName = elementName;
    }

    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        Iterator iterator = getIterator(param);
        environment.getTbasicTarget().setFieldValue(IteratorNextOperation.ITERATOR + elementName, iterator, true);

        return new Result(ReturnType.NEXT, iterator);
    }

    private Iterator getIterator(Object param) {
        if (param.getClass().isArray()) {
            return Arrays.asList((Object[]) param).iterator();
        }

        // Will never happen, as the type of the variable will be checked at
        // compile time.
        // See
        // org.openl.rules.tbasic.compile.AlgorithmCompiler.declareArrayElement()
        //
        throw new IllegalArgumentException("The parameter should be iterable");
    }
}

package org.openl.rules.dt;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A function of the rules that counts how many times a formula calls it.
 *
 * @author Yury Molchan
 */
public final class FormulaCalls {

    static final AtomicInteger CALLS = new AtomicInteger();

    private FormulaCalls() {
    }

    /** Counts the call and gives the value back. */
    public static Integer count(Integer value) {
        CALLS.incrementAndGet();
        return value;
    }
}

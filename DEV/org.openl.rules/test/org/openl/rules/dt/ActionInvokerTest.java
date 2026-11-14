package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.domain.IntArrayIterator;

/**
 * @author Yury Molchan
 */
class ActionInvokerTest {

    /** An action that answers the given value of each rule and nothing for any other rule. */
    private static IBaseAction action(boolean returns, boolean collects, Map<Integer, Object> values) {
        return (IBaseAction) Proxy.newProxyInstance(IBaseAction.class.getClassLoader(),
                new Class<?>[]{IBaseAction.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "executeAction" -> values.get((Integer) args[0]);
                    case "isReturnAction" -> returns;
                    case "isCollectReturnAction" -> collects;
                    case "isCollectReturnKeyAction" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void answersTheRuleThatReturnedOnlyWhenSeveralMatch() {
        var invoker = new ActionInvoker(new IntArrayIterator(new int[]{7, 9}),
                new IBaseAction[]{action(true, false, Map.of(7, 7, 9, 8))},
                false);

        assertEquals(7, invoker.invoke(null, new Object[0], null));
        assertArrayEquals(new int[]{7}, invoker.getResultRules());
        assertArrayEquals(new int[]{7, 9}, invoker.getRules());
    }

    @Test
    void skipsAMatchingRuleThatReturnsNothing() {
        var invoker = new ActionInvoker(new IntArrayIterator(new int[]{3, 5}),
                new IBaseAction[]{action(true, false, Map.of(5, "five"))},
                false);

        assertEquals("five", invoker.invoke(null, new Object[0], null));
        assertArrayEquals(new int[]{5}, invoker.getResultRules());
    }

    @Test
    void answersEveryMatchingRuleWhenNoneReturned() {
        var invoker = new ActionInvoker(new IntArrayIterator(new int[]{1, 2}),
                new IBaseAction[]{action(true, false, Map.of())},
                false);

        invoker.invoke(null, new Object[0], null);
        assertArrayEquals(new int[]{1, 2}, invoker.getResultRules());
    }
}

package org.openl.rules.ruleservice.storelogdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener.Instantiator;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;

/**
 * Checks how a failed store log data advice affects the advices bound to the same step of the call.
 */
class StoreLogDataAdviceFailureTest {

    private final StoreLogDataServiceInvocationAdviceListener listener = listener();

    @AfterEach
    void tearDown() {
        StoreLogDataHolder.remove();
    }

    @Test
    void nextAdviceOfTheStepRunsWhenAnAdviceFailsOnAFailedCall() {
        var failure = new IllegalStateException("The call failed.");

        listener.afterMethodInvocation(rule(), new Object[0], null, failure, instantiator());

        assertEquals(Map.of("collected", "The call failed."), StoreLogDataHolder.get().getCustomValues());
    }

    @Test
    void adviceThatFailsOnAPassedCallFailsTheCall() {
        var rule = rule();
        var args = new Object[0];
        var instantiator = instantiator();

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> listener.afterMethodInvocation(rule, args, "result", null, instantiator));

        assertEquals(FailingAdvice.MESSAGE, thrown.getMessage());
        assertTrue(StoreLogDataHolder.get().getCustomValues().isEmpty(),
                "the advice after the failed one does not run");
    }

    private static StoreLogDataServiceInvocationAdviceListener listener() {
        var manager = mock(StoreLogDataManager.class);
        when(manager.isEnabled()).thenReturn(true);
        when(manager.getServices()).thenReturn(List.of());
        return new StoreLogDataServiceInvocationAdviceListener(manager);
    }

    private static Instantiator instantiator() {
        return new Instantiator() {
            @Override
            public <T> T instantiate(Class<T> clazz) {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private static Method rule() {
        try {
            return Annotated.class.getDeclaredMethod("rule");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    interface Annotated {
        @PrepareStoreLogData(FailingAdvice.class)
        @PrepareStoreLogData(CollectError.class)
        void rule();
    }

    public static class FailingAdvice implements StoreLogDataAdvice {
        static final String MESSAGE = "The advice failed.";

        @Override
        public void prepare(Map<String, Object> values,
                            Object[] args,
                            @Nullable Object result,
                            @Nullable Exception ex) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }

    public static class CollectError implements StoreLogDataAdvice {
        @Override
        public void prepare(Map<String, Object> values,
                            Object[] args,
                            @Nullable Object result,
                            @Nullable Exception ex) {
            values.put("collected", ex == null ? "no error" : ex.getMessage());
        }
    }
}

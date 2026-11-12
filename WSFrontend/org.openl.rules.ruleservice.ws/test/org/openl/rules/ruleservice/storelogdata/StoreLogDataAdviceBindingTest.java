package org.openl.rules.ruleservice.storelogdata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;

/**
 * Checks that an annotation bound to an advice class finds the advice behind a proxy of Spring too.
 *
 * @author Yury Molchan
 */
class StoreLogDataAdviceBindingTest {

    @Test
    void bindsToAnAdviceAsItIsWritten() {
        assertTrue(StoreLogDataServiceInvocationAdviceListener.isBoundTo(binding(), new BeforeAdvice()));
        assertFalse(StoreLogDataServiceInvocationAdviceListener.isBoundTo(binding(), new OtherAdvice()));
    }

    @Test
    void bindsToAnAdviceBehindAProxyOfItsClass() {
        var factory = new ProxyFactory(new BeforeAdvice());
        factory.setProxyTargetClass(true);
        var proxied = (ServiceMethodAdvice) factory.getProxy();
        assertNotEquals(BeforeAdvice.class, proxied.getClass(), "the advice is an instance of a generated class");

        assertTrue(StoreLogDataServiceInvocationAdviceListener.isBoundTo(binding(), proxied));
    }

    private static PrepareStoreLogData binding() {
        try {
            return Annotated.class.getDeclaredMethod("rule").getAnnotation(PrepareStoreLogData.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    interface Annotated {
        @PrepareStoreLogData(value = StoreLogDataAdvice.class, bindToServiceMethodAdvice = BeforeAdvice.class)
        void rule();
    }

    public static class BeforeAdvice implements ServiceMethodAdvice {
    }

    public static class OtherAdvice implements ServiceMethodAdvice {
    }
}

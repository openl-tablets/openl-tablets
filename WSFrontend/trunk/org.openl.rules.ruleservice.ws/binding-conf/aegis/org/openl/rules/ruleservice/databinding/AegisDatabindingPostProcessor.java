package org.openl.rules.ruleservice.databinding;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.openl.rules.context.RuntimeContextBeanType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * Spring bean postprocessor that serves to adjust {@link AegisDatabinding} by
 * specifying own type mappings.
 * 
 * @author PUdalau
 */
public class AegisDatabindingPostProcessor implements BeanPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object arg0, String arg1) throws BeansException {
        if (arg0 instanceof AegisDatabinding) {
            AegisDatabinding databinding = (AegisDatabinding) arg0;
            databinding.getAegisContext()
                .getTypeMapping()
                .register(RuntimeContextBeanType.TYPE_CLASS, RuntimeContextBeanType.QNAME, new RuntimeContextBeanType());
        }
        return arg0;
    }

    @Override
    public Object postProcessBeforeInitialization(Object arg0, String arg1) throws BeansException {
        return arg0;
    }

}

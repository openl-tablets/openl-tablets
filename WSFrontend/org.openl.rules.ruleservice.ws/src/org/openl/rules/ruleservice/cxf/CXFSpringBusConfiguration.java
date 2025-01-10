package org.openl.rules.ruleservice.cxf;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.io.CachedConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CXFSpringBusConfiguration {

    @Bean(name = "cxf", destroyMethod = "shutdown")
    SpringBus cxf() {
        var bus = new SpringBus(true);
        // Disable org.apache.cxf.io.DelayedCachedOutputStreamCleaner due to memory leak
        bus.setProperty(CachedConstants.CLEANER_DELAY_BUS_PROP, 0);
        return bus;
    }

}

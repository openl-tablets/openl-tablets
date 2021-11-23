package org.openl.rules.ruleservice.spring;

import org.apache.cxf.Bus;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registering of CXF MBeans.
 *
 * @author Yury Molchan
 * @see <a href="https://cxf.apache.org/docs/jmx-management.html">CXF JMX Integration</a>
 */
@Configuration
@ConditionalOnEnable("ruleservice.jmx.enabled")
public class CXFManagement {

    @Bean
    CounterRepository counters(Bus cxf) {
        CounterRepository counterRepository = new CounterRepository();
        counterRepository.setBus(cxf);
        return counterRepository;
    }

    @Bean
    InstrumentationManagerImpl manager(Bus cxf) {
        InstrumentationManagerImpl manager = new InstrumentationManagerImpl();
        manager.setBus(cxf);
        manager.setEnabled(true);
        manager.setUsePlatformMBeanServer(true);
        manager.init();
        return manager;
    }
}

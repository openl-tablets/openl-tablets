package org.openl.rules.ruleservice.spring;

import org.apache.cxf.buslifecycle.BusCreationListener;
import org.apache.cxf.io.CachedConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Workaround for the case when a LoggingFeature is used.
 * See implementations in https://issues.apache.org/jira/browse/CXF-7396
 * <p>
 * FIXME: Delete this configuration when a memory leak are introduced by CXF-7396 will be fixed.
 *
 * @author Yury Molchan
 */
@Configuration
public class CXFMemoryLeakFixWhenLoggingFeatureIsUsed {

    @Bean
    BusCreationListener cxfBusConfigurer() {
        return cxf -> cxf.setProperty(CachedConstants.CLEANER_DELAY_BUS_PROP, 0);
    }
}

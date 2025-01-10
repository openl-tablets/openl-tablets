package org.openl.rules.ruleservice.spring;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenLLoggingConfiguration {

    @Bean
    public LoggingFeature payloadLogging() {
        var logging = new LoggingFeature();
        logging.setInMemThreshold(1); // required for easier reproducing of memory leak in DelayedCachedOutputStreamCleaner
        logging.setSender(x -> {}); // do not spam into the log
        return logging;
    }

}

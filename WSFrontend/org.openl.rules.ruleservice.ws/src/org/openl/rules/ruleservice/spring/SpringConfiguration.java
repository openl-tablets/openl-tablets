package org.openl.rules.ruleservice.spring;

import org.apache.cxf.transport.common.gzip.GZIPFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.spring.config.ConditionalOnEnable;

/**
 * Spring Java configuration for using instead of XML configuration.
 *
 * @author Yury Molchan
 */
@Configuration
public class SpringConfiguration {

    @Bean
    @ConditionalOnEnable("ruleservice.gzip.threshold")
    GZIPFeature gzipFeature(@Value("${ruleservice.gzip.threshold}") Integer gzipThreshold) {
        var gzipFeature = new GZIPFeature();
        gzipFeature.setThreshold(gzipThreshold);
        return gzipFeature;
    }

}

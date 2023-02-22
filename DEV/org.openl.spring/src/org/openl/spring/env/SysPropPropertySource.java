package org.openl.spring.env;

import java.util.Map;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * A specialized property source for the system properties are loaded from  {@link System#getProperties()}
 *
 * @author Yury Molchan
 */
public class SysPropPropertySource extends MapPropertySource {
    /**
     * Create a new {@code MapPropertySource} with the given name and {@code Map}.
     *
     * @param source the Map source (without {@code null} values in order to get
     *               consistent {@link #getProperty} and {@link #containsProperty} behavior)
     */
    public SysPropPropertySource(Map<String, Object> source) {
        super(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, source);
        ConfigLog.LOG.info("Loading System Properties parameters: {} properties.", getPropertyNames().length);
    }
}

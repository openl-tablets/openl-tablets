package org.openl.rules.ruleservice.management;

/**
 * Starts rule service.
 *
 * @author Marat Kamalov
 *
 */
public interface ServiceManager {
    /**
     * Determine services to be deployed on start.
     */
    void start();
}

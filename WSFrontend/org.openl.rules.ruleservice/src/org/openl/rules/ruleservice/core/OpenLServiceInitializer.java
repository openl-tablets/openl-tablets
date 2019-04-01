package org.openl.rules.ruleservice.core;

/**
 * Interface to support service compilation if it is requested.
 *
 * @author Marat Kamalov
 *
 */
public interface OpenLServiceInitializer {

    void ensureInitialization(OpenLService openLService) throws RuleServiceInstantiationException;

}

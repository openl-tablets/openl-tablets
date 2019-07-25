package org.openl.rules.ruleservice.core;

/**
 * Interface for OpenL instantiation factory, that is used by RuleService. Implementations should create OpenLService
 * instantiated objects from ServiceDescriptions.
 *
 * @author Marat Kamalov
 *
 */
public interface RuleServiceInstantiationFactory {

    /**
     * Returns fully instantiated OpenLService object from ServiceDesctiption.
     *
     * @param serviceDescription
     * @return
     * @throws RuleServiceInstantiationException
     */
    OpenLService createService(ServiceDescription serviceDescription) throws RuleServiceInstantiationException;
    
    void clean(ServiceDescription serviceDescription);
}

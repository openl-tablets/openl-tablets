package org.openl.rules.ruleservice.core;

public abstract class AbstractOpenLServiceInitializer implements OpenLServiceInitializer {

    private volatile boolean initializated = false;
    private volatile boolean initializationStarted = false;

    @Override
    public void ensureInitialization(OpenLService openLService) throws RuleServiceInstantiationException {
        if (!initializated) {
            synchronized (this) {
                if (!initializated && !initializationStarted) {
                    initializationStarted = true;
                    init(openLService);
                    initializated = true;
                }
            }
        }
    }

    protected void validate(OpenLService openLService) throws RuleServiceInstantiationException {
        if (openLService.getServiceClass().getMethods().length == 0) {
            throw new RuleServiceInstantiationException(
                String.format("Service '%s' does not have any methods to deploy.", openLService.getName()));
        }
    }

    protected abstract void init(OpenLService openLService) throws RuleServiceInstantiationException;

}

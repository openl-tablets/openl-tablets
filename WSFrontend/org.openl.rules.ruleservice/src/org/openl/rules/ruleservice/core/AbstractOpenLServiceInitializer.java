package org.openl.rules.ruleservice.core;

public abstract class AbstractOpenLServiceInitializer implements OpenLServiceInitializer {

    private volatile boolean initialized;
    private volatile boolean initializationStarted;

    @Override
    public void ensureInitialization(OpenLService openLService) throws RuleServiceInstantiationException {
        if (!initialized) {
            synchronized (this) {
                if (!initialized && !initializationStarted) {
                    initializationStarted = true;
                    init(openLService);
                    initialized = true;
                }
            }
        }
    }

    protected abstract void init(OpenLService openLService) throws RuleServiceInstantiationException;

}

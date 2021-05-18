package org.openl.rules.ruleservice.storelogdata;

public abstract class AbstractStoreLogDataService implements StoreLogDataService {
    private boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}

package org.openl.rules.ruleservice.rest;

import org.springframework.beans.factory.InitializingBean;

/**
 * Bean for fetching application configs
 *
 * @author Eugene Biruk
 */
public class ConfigInfoBean implements InitializingBean {

    private boolean deployerEnabled;

    public boolean isDeployerEnabled() {
        return deployerEnabled;
    }

    public void setDeployerEnabled(boolean deployerEnabled) {
        this.deployerEnabled = deployerEnabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}

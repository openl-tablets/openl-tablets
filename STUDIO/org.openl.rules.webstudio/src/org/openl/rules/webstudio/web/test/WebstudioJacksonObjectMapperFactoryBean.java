package org.openl.rules.webstudio.web.test;

import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

public final class WebstudioJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    @Override
    protected void applyAfterProjectConfiguration() {
        setFailOnUnknownProperties(false);
    }
}

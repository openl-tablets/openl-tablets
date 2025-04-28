package org.openl.rules.rest.ui.config;

import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class ReactUIConfiguration {

    @Profile("dev")
    @Bean("reactUiRoot")
    public Supplier<String> getReactUiRootDev(@Value("${webstudio.dev.external.ui.uri}") String externalUiUri) {
        return () -> externalUiUri;
    }

    @Profile("!dev")
    @Bean("reactUiRoot")
    @RequestScope
    public Supplier<String> getReactUiRoot(HttpServletRequest request) {
        return () -> request.getContextPath() + "/javascript/ui";
    }

}

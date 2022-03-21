package org.openl.rules.spring.openapi.conf;

import java.util.Collections;

import org.openl.rules.spring.openapi.OpenApiSpringMvcReader;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.controller.OpenApiController;
import org.openl.rules.spring.openapi.service.OpenApiService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(basePackages = {"org.openl.rules.spring.openapi.controller", "org.openl.rules.spring.openapi.service"})
public class SpringMvcOpenApiConfiguration {

    @Bean
    public SpringMvcHandlerMethodsHelper springMvcHandlerMethodsHelper(ApplicationContext context) {
        return new SpringMvcHandlerMethodsHelper(context);
    }

    @Bean
    public OpenApiService openApiService(ApplicationContext context,
            SpringMvcHandlerMethodsHelper mvcHandlerMethodsHelper) {
        return new OpenApiService(context, mvcHandlerMethodsHelper, Collections.singleton(OpenApiController.class));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public OpenApiSpringMvcReader openApiSpringRestControllerWalker(SpringMvcHandlerMethodsHelper methodsHelper) {
        return new OpenApiSpringMvcReader(methodsHelper);
    }


}

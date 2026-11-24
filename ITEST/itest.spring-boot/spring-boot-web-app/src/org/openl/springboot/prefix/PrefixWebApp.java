package org.openl.springboot.prefix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.ruleservice.servlet.CXFServlet;
import org.openl.rules.ruleservice.servlet.RuleServicesFilter;
import org.openl.rules.ruleservice.servlet.SpringInitializer;

/**
 * OpenL Rule Services under the {@code /rules} path, beside the controllers of Spring MVC, as the Rule Services
 * guide shows it.
 *
 * @author Yury Molchan
 */
@SpringBootApplication
@RestController
public class PrefixWebApp {

    public static void main(String[] args) {
        SpringApplication.run(PrefixWebApp.class, args);
    }

    @GetMapping("/mvc/hello")
    public String hello() {
        return "Hello from Spring MVC";
    }

    @Bean
    ServletListenerRegistrationBean<SpringInitializer> openlContext() {
        return new ServletListenerRegistrationBean<>(new SpringInitializer());
    }

    @Bean
    ServletRegistrationBean<CXFServlet> openlServlet() {
        var registration = new ServletRegistrationBean<>(new CXFServlet(), "/rules/*");
        registration.setLoadOnStartup(1);
        registration.addInitParameter("service-list-path", "cxf-services");
        registration.addInitParameter("hide-service-list-page", "true");
        return registration;
    }

    @Bean
    FilterRegistrationBean<RuleServicesFilter> openlFilter() {
        var registration = new FilterRegistrationBean<>(new RuleServicesFilter());
        registration.addUrlPatterns("/rules/*");
        registration.addInitParameter("xForwardedPrefixStrategy", "PREPEND");
        return registration;
    }
}

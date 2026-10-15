package spring

import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

import org.openl.itest.proxies.JavaProxiesConfig
import org.openl.rules.project.model.ProjectDescriptor
import org.openl.rules.project.model.RulesDeploy

/**
 * This is a Spring Configuration is loaded in the service context.
 * It creates a bean which is set a static 'result' field, which is used in test.
 * The imported JavaProxiesConfig proxies its beans and the Groovy 'greeting' bean by class.
 */
@Configuration
@Import(JavaProxiesConfig)
class SpringConfig {

    static String result

    static String proxies

    @Bean
    String feature(RulesDeploy rulesDeploy, ProjectDescriptor projectDescriptor) {
        result = "pong!"
    }

    @Bean
    Greeting greeting() {
        new Greeting()
    }

    @Bean
    String proxiesReport(Greeting greeting, @Qualifier("report") String report) {
        proxies = "${report}; ${greeting.greet("Groovy")}, proxied by class: ${AopUtils.isCglibProxy(greeting)}"
    }

    // Called from the OpenL rules
    static String pong() {
        result
    }

    // Called from the OpenL rules
    static String proxies() {
        proxies
    }
}

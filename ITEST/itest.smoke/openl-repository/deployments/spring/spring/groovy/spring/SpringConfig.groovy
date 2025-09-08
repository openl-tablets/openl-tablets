package spring

import org.openl.rules.project.model.ProjectDescriptor
import org.openl.rules.project.model.RulesDeploy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * This is a Spring Configuration is loaded in the service context.
 * It creates a bean which is set a static 'result' field, which is used in test.
 */
@Configuration
class SpringConfig {

    static String result

    @Bean
    String feature(RulesDeploy rulesDeploy, ProjectDescriptor projectDescriptor) {
        result = "pong!"
    }

    // Called from the OpenL rules
    static String pong() {
        result
    }
}

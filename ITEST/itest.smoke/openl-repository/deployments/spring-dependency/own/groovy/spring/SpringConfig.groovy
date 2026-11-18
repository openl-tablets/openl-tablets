package spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The Spring configuration of the deployed project. It configures its service instead of the one of the dependency.
 */
@Configuration
class SpringConfig {

    @Bean
    String springConfigOwner() {
        "own"
    }
}

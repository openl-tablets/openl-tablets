package spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The Spring configuration of a project with empty publishers, which the single publisher still publishes.
 */
@Configuration
class SpringConfig {

    @Bean
    String springConfigOwner() {
        "stub"
    }
}

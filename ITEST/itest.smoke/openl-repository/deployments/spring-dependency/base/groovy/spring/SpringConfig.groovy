package spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The Spring configuration of the dependency project, which publishes no service. It never applies, neither to its
 * own service nor to the services of the projects using it: wherever it did, that service would fail to start.
 */
@Configuration
class SpringConfig {

    @Bean
    String springConfigOwner() {
        throw new IllegalStateException("The Spring configuration of a dependency project is applied.")
    }
}

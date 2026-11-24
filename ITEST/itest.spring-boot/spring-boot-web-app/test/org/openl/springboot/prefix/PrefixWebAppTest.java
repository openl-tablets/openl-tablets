package org.openl.springboot.prefix;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * @author Yury Molchan
 */
@SpringBootTest(classes = PrefixWebApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class PrefixWebAppTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void springMvcBesideRuleServices() {
        assertThat(restTemplate.getForObject("/mvc/hello", String.class)).isEqualTo("Hello from Spring MVC");
    }

    @Test
    void rulesUnderPrefix() {
        assertThat(restTemplate.postForObject("/rules/openl-rules-rs/SayHello", "Mister", String.class))
                .isEqualTo("Hello, Mister!");
        assertThat(restTemplate.getForObject("/rules/admin/ui/info", String.class))
                .contains("\"deploymentName\":\"itest.spring-boot.openl-rules");
    }

    @Test
    void openApiNamesThePrefix() {
        assertThat(restTemplate.getForObject("/rules/openl-rules-rs/openapi.json", String.class))
                .contains("/rules/openl-rules-rs\"");
    }

    @Test
    void welcomePageUnderPrefix() {
        assertThat(restTemplate.getForObject("/rules/", String.class)).contains("OpenL Tablets Rule Services");
    }
}

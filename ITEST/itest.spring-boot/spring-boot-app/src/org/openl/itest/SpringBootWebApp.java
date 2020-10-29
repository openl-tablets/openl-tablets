package org.openl.itest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan("org.openl.rules.ruleservice.servlet")
public class SpringBootWebApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebApp.class, args);
    }
}

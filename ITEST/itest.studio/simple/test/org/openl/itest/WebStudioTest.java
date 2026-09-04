package org.openl.itest;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    private static final String EPBDS_16518_SOURCE_RULES = """
            <project>
                <name>EPBDS-16518 Source</name>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="Main.xlsx"/>
                    </module>
                    <module>
                        <name>Secondary</name>
                        <rules-root path="Main2.xlsx"/>
                    </module>
                </modules>
                <dependencies>
                    <dependency>
                        <name>EPBDS-16518 Target</name>
                    </dependency>
                </dependencies>
            </project>
            """;
    private static final String EPBDS_16518_TARGET_RULES = """
            <project>
                <name>EPBDS-16518 Target</name>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="Main.xlsx"/>
                    </module>
                </modules>
            </project>
            """;

    @Test
    void simple() throws Exception {
        seedLocalProject();
        try (var client = JettyServer.get().start()) {
            client.test("test-resources-simple");
        }
    }

    private static void seedLocalProject() throws IOException {
        var configured = System.getProperty("openl.home", "target/openl-test-${openl.start.milli}");
        var home = Path.of(configured.replace("${openl.start.milli}", String.valueOf(System.currentTimeMillis())));
        System.setProperty("openl.home", home.toString());
        var userDir = home.resolve("user-workspace").resolve("jdoe");
        var projectZip = Path.of("test-resources-simple", "EPBDS-15952", "EPBDS-15952-Project.zip");
        LocalProjectFixture.seed(userDir,
                "EPBDS-15952-Project",
                projectZip);
        LocalProjectFixture.seed(userDir, "epbds-16518-source", projectZip, EPBDS_16518_SOURCE_RULES);
        LocalProjectFixture.seed(userDir, "epbds-16518-target", projectZip, EPBDS_16518_TARGET_RULES);
    }
}

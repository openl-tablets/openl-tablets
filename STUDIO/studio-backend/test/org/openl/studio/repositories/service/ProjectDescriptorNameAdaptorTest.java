package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * @author Yury Molchan
 */
class ProjectDescriptorNameAdaptorTest {

    private static final String RULES_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
              <!-- Some Comment -->
              <name>MyProject</name>
                <modules>
                  <module>
                    <name>MyProject</name>
                    <rules-root path="MyProject.xlsx"/>
                  </module>
                </modules>
            </project>
            """;

    private static String adapt(String projectName, String rulesXml) throws IOException {
        var adaptor = new ProjectDescriptorNameAdaptor(projectName);
        try (var in = adaptor.apply(new ByteArrayInputStream(rulesXml.getBytes(StandardCharsets.UTF_8)))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String nameOf(String rulesXml) {
        return ProjectDescriptor.read(new ByteArrayInputStream(rulesXml.getBytes(StandardCharsets.UTF_8))).getName();
    }

    @Test
    void leavesTheFileAsItIsWhenItHoldsTheName() throws IOException {
        assertEquals(RULES_XML, adapt("MyProject", RULES_XML));
    }

    @Test
    void changesOnlyTheProjectNameAndKeepsTheComments() throws IOException {
        var adapted = adapt("New & <Improved>", RULES_XML);

        assertEquals(RULES_XML.replaceFirst("<name>MyProject</name>", "<name>New &amp; &lt;Improved&gt;</name>"),
                adapted);
        assertEquals("New & <Improved>", nameOf(adapted));
        assertTrue(adapted.contains("<!-- Some Comment -->"));
    }

    @Test
    void writesAFileWithoutANameAnew() throws IOException {
        var withoutName = RULES_XML.replace("  <name>MyProject</name>\n", "");

        var adapted = adapt("Named", withoutName);

        assertEquals("Named", nameOf(adapted));
        assertFalse(adapted.contains("Some Comment"));
    }

    @Test
    void leavesAFileThatCannotBeReadAsItIs() throws IOException {
        assertEquals("not xml", adapt("Named", "not xml"));
    }

    @Test
    void acceptsTheRulesXmlOfTheProjectRootOnly() {
        var adaptor = new ProjectDescriptorNameAdaptor("Named");

        assertTrue(adaptor.accept(Path.of("rules.xml")));
        assertTrue(adaptor.accept(Path.of("/rules.xml")));
        assertFalse(adaptor.accept(Path.of("nested/rules.xml")));
    }
}

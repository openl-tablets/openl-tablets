package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GroovyJakartaMigratorTest {

    static Stream<Arguments> rewritesImports() {
        return Stream.of(
                argumentSet("JAX-RS imports", """
                        import javax.ws.rs.GET
                        import javax.ws.rs.Path
                        import javax.ws.rs.PathParam
                        import javax.ws.rs.Produces
                        import javax.ws.rs.core.MediaType
                        import javax.ws.rs.core.Response

                        interface Service {}
                        """, """
                        import jakarta.ws.rs.GET
                        import jakarta.ws.rs.Path
                        import jakarta.ws.rs.PathParam
                        import jakarta.ws.rs.Produces
                        import jakarta.ws.rs.core.MediaType
                        import jakarta.ws.rs.core.Response

                        interface Service {}
                        """),
                argumentSet("JAXB imports", """
                        import javax.xml.bind.annotation.XmlAccessType;
                        import javax.xml.bind.annotation.XmlAccessorType;
                        import javax.xml.bind.annotation.XmlElement;
                        import javax.xml.bind.annotation.XmlRootElement;
                        import javax.xml.bind.annotation.XmlType;

                        @XmlType(name = "Bean")
                        class Bean {}
                        """, """
                        import jakarta.xml.bind.annotation.XmlAccessType;
                        import jakarta.xml.bind.annotation.XmlAccessorType;
                        import jakarta.xml.bind.annotation.XmlElement;
                        import jakarta.xml.bind.annotation.XmlRootElement;
                        import jakarta.xml.bind.annotation.XmlType;

                        @XmlType(name = "Bean")
                        class Bean {}
                        """),
                argumentSet("all supported Jakarta EE 10 namespaces", """
                        import javax.ws.rs.GET
                        import javax.ws.rs.core.Response
                        import javax.xml.bind.annotation.XmlElement
                        import javax.persistence.Entity
                        import javax.persistence.criteria.CriteriaQuery
                        import javax.validation.constraints.NotNull
                        import javax.validation.groups.Default
                        import javax.servlet.http.HttpServletRequest
                        import javax.inject.Inject
                        """, """
                        import jakarta.ws.rs.GET
                        import jakarta.ws.rs.core.Response
                        import jakarta.xml.bind.annotation.XmlElement
                        import jakarta.persistence.Entity
                        import jakarta.persistence.criteria.CriteriaQuery
                        import jakarta.validation.constraints.NotNull
                        import jakarta.validation.groups.Default
                        import jakarta.servlet.http.HttpServletRequest
                        import jakarta.inject.Inject
                        """));
    }

    @ParameterizedTest
    @MethodSource
    void rewritesImports(String src, String expected) {
        var migrated = GroovyJakartaMigrator.migrate(src);

        assertEquals(expected, migrated);
    }

    @Test
    void rewritesWildcardImport() {
        var src = "import javax.ws.rs.*\n";

        assertEquals("import jakarta.ws.rs.*\n", GroovyJakartaMigrator.migrate(src));
    }

    @Test
    void rewritesFullyQualifiedReferenceMidLine() {
        var src = "Response r = javax.ws.rs.core.Response.ok().build()\n";

        assertEquals("Response r = jakarta.ws.rs.core.Response.ok().build()\n",
                GroovyJakartaMigrator.migrate(src));
    }

    static Stream<Arguments> leavesSourceUnchanged() {
        return Stream.of(
                // javax.annotation overlaps with Java SE javax.annotation.processing.* — skipped to avoid breakage.
                // javax.transaction.xa.* and most javax.security.* live in Java SE — likewise skipped.
                argumentSet("ambiguous javax packages", """
                        import javax.annotation.processing.Processor
                        import javax.transaction.xa.Xid
                        import javax.security.auth.Subject
                        import javax.sql.DataSource
                        import javax.crypto.Cipher
                        """),
                // Jakarta EE 10 also moves these namespaces, but they are unused in OpenL Tablets groovy scripts and
                // therefore intentionally out of the migrator's scope.
                argumentSet("out-of-scope Jakarta namespaces", """
                        import javax.enterprise.context.ApplicationScoped
                        import javax.faces.bean.ManagedBean
                        import javax.mail.Session
                        import javax.el.ELContext
                        """),
                argumentSet("already Jakarta source", """
                        import jakarta.ws.rs.GET
                        import jakarta.xml.bind.annotation.XmlElement
                        """));
    }

    @ParameterizedTest
    @MethodSource
    void leavesSourceUnchanged(String src) {
        assertEquals(src, GroovyJakartaMigrator.migrate(src));
    }

    @Test
    void returnsSameStringWhenNoChangesNeeded() {
        var src = "class Empty {}\n";

        assertSame(src, GroovyJakartaMigrator.migrate(src));
    }

    @Test
    void rewritesAllGroovyFilesUnderProjectFolder(@TempDir Path projectFolder) throws IOException {
        Path service = projectFolder.resolve("groovy/Service.groovy");
        Files.createDirectories(service.getParent());
        Files.writeString(service, """
                import javax.ws.rs.GET
                import javax.ws.rs.Path

                interface Service {
                    @GET @Path("/x") String x()
                }
                """, StandardCharsets.UTF_8);
        Path bean = projectFolder.resolve("classes/Bean.groovy");
        Files.createDirectories(bean.getParent());
        Files.writeString(bean, """
                import javax.xml.bind.annotation.XmlElement;

                class Bean {
                    @XmlElement String name
                }
                """, StandardCharsets.UTF_8);

        new GroovyJakartaMigrator().migrate(projectFolder, null);

        assertEquals("""
                import jakarta.ws.rs.GET
                import jakarta.ws.rs.Path

                interface Service {
                    @GET @Path("/x") String x()
                }
                """, Files.readString(service, StandardCharsets.UTF_8));
        assertEquals("""
                import jakarta.xml.bind.annotation.XmlElement;

                class Bean {
                    @XmlElement String name
                }
                """, Files.readString(bean, StandardCharsets.UTF_8));
    }

    @Test
    void leavesNonGroovyFilesUntouched(@TempDir Path projectFolder) throws IOException {
        var java = projectFolder.resolve("Bean.java");
        var content = "import javax.ws.rs.GET;\nclass Bean {}\n";
        Files.writeString(java, content, StandardCharsets.UTF_8);

        new GroovyJakartaMigrator().migrate(projectFolder, null);

        assertEquals(content, Files.readString(java, StandardCharsets.UTF_8));
    }

    @Test
    void skipsMissingFolder(@TempDir Path tmp) throws IOException {
        var missing = tmp.resolve("does-not-exist");

        new GroovyJakartaMigrator().migrate(missing, null);

        assertFalse(Files.exists(missing));
    }

    @Test
    void leavesUntouchedGroovyFileContentIntact(@TempDir Path projectFolder) throws IOException {
        Path file = projectFolder.resolve("classes/Plain.groovy");
        Files.createDirectories(file.getParent());
        var content = "class Plain {}\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        new GroovyJakartaMigrator().migrate(projectFolder, null);

        assertEquals(content, Files.readString(file, StandardCharsets.UTF_8));
    }
}

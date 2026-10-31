package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroovyGenericStaticMigratorTest {

    @Test
    void removesTypeArgumentsOfStaticMembers() {
        assertEquals("""
                Literal<Node> x = Literal.of(true)
                def m = Map.Entry.comparingByKey()
                def e = Optional.empty()
                def n = Holder.DEFAULT
                def o = Optional.empty()
                """, GroovyGenericStaticMigrator.migrate("""
                Literal<Node> x = Literal<Node>.of(true)
                def m = Map.Entry<String, List<? extends Number[]>>.comparingByKey()
                def e = Optional<Map<String, Integer>>.empty()
                def n = Holder<String>.DEFAULT
                def o = Optional<Holder<String>.X>.empty()
                """));
    }

    @Test
    void keepsTypesAndComparisons() {
        var source = """
                List<String> names = new ArrayList<String>()
                Map<String, Integer> sizes = [:]
                def less = a < b && c > d.e
                def generic = Holder<String>
                def call = List<String>()
                def cast = (List<String>) value
                def lower = holder<x>.y
                def text = 'Literal<Node>.of(true)' + "Map<K, V>.of()" + '''Optional<T>.empty()'''
                // Literal<Node>.of(true)
                /* Holder<String>.DEFAULT */
                def escaped = "a \\" Literal<Node>.of(true)"
                """;
        assertEquals(source, GroovyGenericStaticMigrator.migrate(source));
    }

    @Test
    void rewritesGroovyFiles(@TempDir Path folder) throws IOException {
        var file = Files.createDirectories(folder.resolve("groovy")).resolve("Builder.groovy");
        Files.writeString(file, "class Builder { def x = Literal<Node>.of(true) }\n");
        var other = folder.resolve("groovy/Plain.groovy");
        Files.writeString(other, "class Plain {}\n");

        var changed = new GroovyGenericStaticMigrator().migrate(folder, null);

        assertEquals(List.of(file), changed);
        assertEquals("class Builder { def x = Literal.of(true) }\n", Files.readString(file));
    }
}

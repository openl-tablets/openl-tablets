package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.migration.RulesXmlMigrations;

/**
 * {@code rules.xml} migration: drops the {@code <classpath>} block when every entry it contains is a
 * path the OpenL resolver already adds implicitly — {@code groovy/}, {@code groovy}, or {@code lib/*.jar}.
 * The transform is shared with OpenL Studio in {@link RulesXmlMigrations#classpath}.
 * <p>
 * Migrator id: {@code config.project.classpath}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectClasspathMigrator implements Migrator {

    @Override
    public String getId() {
        return "config.project.classpath";
    }

    @Override
    public String getCommitMessage() {
        return "drop default classpath from rules.xml";
    }

    @Override
    public String getDescription() {
        return """
                Drops the entire <classpath> block from rules.xml when every entry is a path OpenL
                already picks up automatically: groovy/, or lib/*.jar. Any unknown entry keeps
                the block intact. After removal the project still finds the same classes in the same
                folders — only the redundant declaration goes away.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, RulesXmlMigrations::classpath);
    }
}

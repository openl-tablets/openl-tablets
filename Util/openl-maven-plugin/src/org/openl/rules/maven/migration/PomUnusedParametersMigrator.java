package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code pom.xml} migration: removes the {@code openl-maven-plugin} parameters that have no effect. These are the
 * parameters that no goal of the plugin declares any more, and the deprecated ones that are ignored, such as
 * {@code deploymentName} and {@code deploymentPackage}.
 * <p>
 * The file is edited as text, so the rest of it keeps its formatting. A pom-less project has no {@code pom.xml} of
 * its own, and the migrator leaves it alone.
 * <p>
 * Migrator id: {@code pom.unused-parameters}.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class PomUnusedParametersMigrator implements Migrator {

    @Override
    public String getId() {
        return "pom.unused-parameters";
    }

    @Override
    public String getCommitMessage() {
        return "drop unused openl-maven-plugin parameters";
    }

    @Override
    public String getDescription() {
        return """
                Removes from the openl-maven-plugin configuration in pom.xml the parameters that have no
                effect: the ones the plugin no longer declares and the deprecated ones it ignores, such as
                deploymentName and deploymentPackage. A configuration left empty goes too. The build keeps
                behaving the same, and the rest of pom.xml keeps its formatting. Pom-less projects are skipped.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        var pom = context.pom();
        if (!Files.isRegularFile(pom)) {
            return List.of();
        }
        // ISO-8859-1 maps every byte to one char, so the bytes outside the removed parameters are written back as
        // they were, whatever the encoding of the file.
        var text = Files.readString(pom, StandardCharsets.ISO_8859_1);
        var edited = PomPluginConfig.removeParameters(text, context.pluginParameters()::contains);
        if (edited.equals(text)) {
            return List.of();
        }
        log.info("Migrate: {} ({})", pom.getFileName(), getCommitMessage());
        Files.writeString(pom, edited, StandardCharsets.ISO_8859_1);
        return List.of(pom);
    }
}

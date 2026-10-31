package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.openl.rules.project.migration.RulesDeployMigrations;

/**
 * {@code rules-deploy.xml} migration: replaces the Jackson typing settings removed in OpenL Tablets 7.0.0. The
 * {@code EVERYTHING} default typing mode becomes {@code NON_FINAL_AND_ENUMS}, and
 * {@code jackson.simpleClassNameAsTypingPropertyValue} gives way to the {@code NAME} type id it stood for. The
 * transform is shared with OpenL Studio via {@link RulesDeployMigrations#jacksonTyping}.
 * <p>
 * Migrator id: {@code config.deploy.jackson-typing}.
 *
 * @author Yury Molchan
 */
public final class ConfigDeployJacksonTypingMigrator extends AbstractMigrator {

    public ConfigDeployJacksonTypingMigrator() {
        super("config.deploy.jackson-typing",
                "replace the Jackson typing settings removed in OpenL 7.0.0",
                """
                Replaces the Jackson settings in rules-deploy.xml that stop a service from deploying or
                are no longer read. jackson.defaultTypingMode EVERYTHING becomes NON_FINAL_AND_ENUMS,
                the closest mode: values of final classes, such as Long, no longer carry their type.
                jackson.simpleClassNameAsTypingPropertyValue is dropped; when it was true, a CLASS or
                absent jackson.jsonTypeInfoId becomes NAME, so the service keeps simple class names.
                """);
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        return ConfigDeployIO.roundtrip(this, sourceFolder, RulesDeployMigrations::jacksonTyping);
    }
}

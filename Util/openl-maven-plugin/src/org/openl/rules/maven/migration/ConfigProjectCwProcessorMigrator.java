package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * {@code rules.xml} migration: drops the legacy
 * {@code org.openl.rules.project.resolving.CWPropertyFileNameProcessor} reference. Any other custom
 * {@code <properties-file-name-processor>} is preserved.
 * <p>
 * Migrator id: {@code config.project.cw-processor}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectCwProcessorMigrator implements Migrator {

    private static final String CW_PROCESSOR = "org.openl.rules.project.resolving.CWPropertyFileNameProcessor";

    /**
     * Mutates {@code descriptor} so the JAXB serializer omits the deprecated CW processor reference. Other
     * custom processor references are left untouched. Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor) {
        if (CW_PROCESSOR.equals(descriptor.getPropertiesFileNameProcessor())) {
            descriptor.setPropertiesFileNameProcessor(null);
        }
    }

    @Override
    public String getId() {
        return "config.project.cw-processor";
    }

    @Override
    public String getCommitMessage() {
        return "drop CWPropertyFileNameProcessor";
    }

    @Override
    public String getDescription() {
        return """
                Removes the discontinued org.openl.rules.project.resolving.CWPropertyFileNameProcessor
                reference from rules.xml's <properties-file-name-processor>. Any other custom processor
                class is preserved — only the specific CW class is dropped.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, ConfigProjectCwProcessorMigrator::transform);
    }
}

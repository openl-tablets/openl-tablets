package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;

/**
 * {@code rules.xml} migration: drops the {@code <classpath>} block when every entry it contains is a
 * path the OpenL resolver already adds implicitly — {@code groovy/} or {@code groovy}. One or several
 * of those entries (in any combination) is enough to trigger the drop; any other entry keeps the
 * whole block.
 * <p>
 * Migrator id: {@code config.project.classpath}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectClasspathMigrator implements Migrator {

    private static final Set<String> DEFAULT_CLASSPATH_PATHS = Set.of("groovy/", "groovy", "groovy\\");

    /**
     * Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor) {
        var classpath = descriptor.getClasspath();
        if (CollectionUtils.isEmpty(classpath)) {
            return;
        }
        boolean allDefaults = classpath.stream()
                .allMatch(e -> e != null && DEFAULT_CLASSPATH_PATHS.contains(e));
        if (allDefaults) {
            descriptor.setClasspath(null);
        }
    }

    @Override
    public String getId() {
        return "config.project.classpath";
    }

    @Override
    public String getCommitMessage() {
        return "drop default classpath from rules.xml";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, ConfigProjectClasspathMigrator::transform);
    }
}

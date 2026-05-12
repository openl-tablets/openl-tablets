package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;

/**
 * {@code rules.xml} migration: drops the {@code <entry path="lib/*.jar"/>} classpath entry when the
 * {@code openl:package} goal would not put anything into the {@code lib/} folder. Mirrors the
 * "{@code lib/}" packaging branch of {@code PackageMojo} (lines 240-246): if neither the project's
 * own artifact ({@code dependencyLib}) nor any filtered non-OpenL dependency would land in
 * {@code lib/}, the wildcard entry is meaningless and is removed. Other classpath entries pass
 * through untouched.
 * <p>
 * The "would lib be populated?" decision is provided by {@link MigrateMojo} via a
 * {@link BooleanSupplier}; the static {@link #transform(ProjectDescriptor, boolean)} method exists so
 * unit tests can drive both branches without a Maven runtime.
 * <p>
 * Run this migrator <strong>before</strong> {@link ConfigProjectClasspathMigrator} — once the
 * {@code lib/*.jar} entry is gone, the classpath may collapse to just the {@code groovy/}/{@code groovy}
 * defaults that the classpath migrator then drops entirely.
 * <p>
 * Migrator id: {@code config.project.lib}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectLibMigrator implements Migrator {

    private static final String LIB_JAR_PATH = "lib/*.jar";

    private final BooleanSupplier packagesLibDependencies;

    public ConfigProjectLibMigrator(BooleanSupplier packagesLibDependencies) {
        this.packagesLibDependencies = packagesLibDependencies;
    }

    /**
     * Package-private for direct unit testing. When {@code packagesLibDependencies} is {@code true} the
     * entry is kept (the package goal would populate {@code lib/}); otherwise the entry is removed.
     */
    static void transform(ProjectDescriptor descriptor, boolean packagesLibDependencies) {
        if (packagesLibDependencies) {
            return;
        }
        var classpath = descriptor.getClasspath();
        if (CollectionUtils.isEmpty(classpath)) {
            return;
        }
        // Match the runtime's separator handling (ProjectDescriptor.processClasspathPathPatterns
        // normalizes '\\' to '/'), so Windows-style "lib\\*.jar" is treated as equivalent.
        classpath.removeIf(entry -> entry != null && LIB_JAR_PATH.equals(entry.replace('\\', '/')));
    }

    @Override
    public String getId() {
        return "config.project.lib";
    }

    @Override
    public String getCommitMessage() {
        return "drop lib/*.jar classpath entry — packaging does not populate lib/";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder,
                descriptor -> transform(descriptor, packagesLibDependencies.getAsBoolean()));
    }
}

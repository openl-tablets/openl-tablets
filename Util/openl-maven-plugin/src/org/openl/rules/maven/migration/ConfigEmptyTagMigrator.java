package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * First migration phase: empty-tag cleanup for both {@code rules.xml} and {@code rules-deploy.xml}.
 * <p>
 * The actual cleanup is performed by JAXB {@code beforeMarshal} callbacks in
 * {@link org.openl.rules.project.model.ProjectDescriptor},
 * {@link org.openl.rules.project.model.RulesDeploy}, {@link org.openl.rules.project.model.Module},
 * {@link org.openl.rules.project.model.OpenAPI}, {@link org.openl.rules.project.model.ExposedMethods},
 * and {@link org.openl.rules.project.model.MethodFilter} — this migrator only triggers the JAXB roundtrip
 * (read → marshal → compare → write) so the cleaned output replaces the on-disk file.
 * <p>
 * No structural rewrites and no deprecated-tag handling — those are the responsibility of the per-file,
 * content-specific migrators ({@code config.project.*}, {@code config.deploy.*}).
 * <p>
 * Migrator id: {@code config.empty-tag}.
 *
 * @author Yury Molchan
 */
public final class ConfigEmptyTagMigrator implements Migrator {

    @Override
    public String getId() {
        return "config.empty-tag";
    }

    @Override
    public String getCommitMessage() {
        return "needless tags cleanup";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        var changed = new ArrayList<Path>();
        changed.addAll(ConfigProjectIO.roundtrip(this, sourceFolder, descriptor -> {}));
        changed.addAll(ConfigDeployIO.roundtrip(this, sourceFolder, rulesDeploy -> {}));
        return List.copyOf(changed);
    }
}

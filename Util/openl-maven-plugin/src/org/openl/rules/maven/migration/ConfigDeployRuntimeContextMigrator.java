package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.model.RulesDeploy;

/**
 * {@code rules-deploy.xml} migration: drops {@code <isProvideRuntimeContext>false</…>} because that is the
 * runtime default. An explicit {@code true} or {@code null} value is left untouched.
 * <p>
 * Migrator id: {@code config.deploy.runtime-context}.
 *
 * @author Yury Molchan
 */
public final class ConfigDeployRuntimeContextMigrator implements Migrator {

    /**
     * Package-private for direct unit testing.
     */
    static void transform(RulesDeploy rulesDeploy) {
        if (Boolean.FALSE.equals(rulesDeploy.isProvideRuntimeContext())) {
            rulesDeploy.setProvideRuntimeContext(null);
        }
    }

    @Override
    public String getId() {
        return "config.deploy.runtime-context";
    }

    @Override
    public String getCommitMessage() {
        return "drop default isProvideRuntimeContext=false";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigDeployIO.roundtrip(this, sourceFolder, ConfigDeployRuntimeContextMigrator::transform);
    }
}

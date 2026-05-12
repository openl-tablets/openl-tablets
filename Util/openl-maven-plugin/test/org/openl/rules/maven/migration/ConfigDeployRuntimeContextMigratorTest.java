package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;

class ConfigDeployRuntimeContextMigratorTest {

    @Test
    void dropsIsProvideRuntimeContextFalse() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);

        ConfigDeployRuntimeContextMigrator.transform(deploy);

        assertNull(deploy.isProvideRuntimeContext());
    }

    @Test
    void keepsIsProvideRuntimeContextTrue() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(true);

        ConfigDeployRuntimeContextMigrator.transform(deploy);

        assertEquals(Boolean.TRUE, deploy.isProvideRuntimeContext());
    }

    @Test
    void leavesIsProvideRuntimeContextNullUnchanged() {
        RulesDeploy deploy = new RulesDeploy();

        ConfigDeployRuntimeContextMigrator.transform(deploy);

        assertNull(deploy.isProvideRuntimeContext());
    }
}

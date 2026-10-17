package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.impl.GroovyScriptFile;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.project.migration.RulesDeployMigrations;
import org.openl.rules.project.model.RulesDeploy;

/**
 * @author Yury Molchan
 */
class OpenAPIHelperTest {

    private static final String TEMPLATE_CLASS = "org.openl.generated.services.Service";

    private final OpenAPIHelper helper = new OpenAPIHelper();

    @Test
    void leavesOutRuntimeContextFlagWhenNoPathUsesIt() throws IOException {
        var bytes = rulesDeploy(false, null);

        var written = new String(bytes);
        assertFalse(written.contains("isProvideRuntimeContext"), written);
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(bytes));
        assertNull(rulesDeploy.isProvideRuntimeContext());
        assertEquals(TEMPLATE_CLASS, rulesDeploy.getAnnotationTemplateClassName());
        assertArrayEquals(new RulesDeploy.PublisherType[]{RulesDeploy.PublisherType.RESTFUL},
                rulesDeploy.getPublishers());
        assertNothingToMigrate(bytes);
    }

    @Test
    void writesRuntimeContextFlagWhenAPathUsesIt() throws IOException {
        var bytes = rulesDeploy(true, null);

        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(bytes));
        assertEquals(Boolean.TRUE, rulesDeploy.isProvideRuntimeContext());
        assertNothingToMigrate(bytes);
    }

    @Test
    void dropsRuntimeContextFlagOfExistingDescriptorWhenNoPathUsesIt() throws IOException {
        var existing = new RulesDeploy();
        existing.setProvideRuntimeContext(true);
        existing.setServiceName("kept");

        var bytes = rulesDeploy(false, existing);

        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(bytes));
        assertNull(rulesDeploy.isProvideRuntimeContext());
        assertEquals("kept", rulesDeploy.getServiceName());
        assertNothingToMigrate(bytes);
    }

    private byte[] rulesDeploy(boolean runtimeContextProvided, @Nullable RulesDeploy existing) throws IOException {
        var model = new ProjectModel("project", runtimeContextProvided, Set.of(), null, null, null);
        var generated = new OpenAPIGeneratedClasses(new GroovyScriptFile(TEMPLATE_CLASS, ""), Set.of());
        try (var in = helper.editOrCreateRulesDeploy(model, generated, existing)) {
            return in.readAllBytes();
        }
    }

    /** OpenL Studio offers Migrate when the migrations would change the descriptor. */
    private static void assertNothingToMigrate(byte[] bytes) {
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(bytes));
        var declared = rulesDeploy.toBytes();
        RulesDeployMigrations.apply(rulesDeploy);
        assertArrayEquals(declared, rulesDeploy.toBytes());
    }
}

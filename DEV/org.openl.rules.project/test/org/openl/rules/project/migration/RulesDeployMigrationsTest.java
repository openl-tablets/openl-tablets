package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;

class RulesDeployMigrationsTest {

    @Test
    void dropsIsProvideRuntimeContextFalse() {
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);

        RulesDeployMigrations.runtimeContext(deploy);

        assertNull(deploy.isProvideRuntimeContext());
    }

    @Test
    void keepsIsProvideRuntimeContextTrue() {
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(true);

        RulesDeployMigrations.runtimeContext(deploy);

        assertEquals(Boolean.TRUE, deploy.isProvideRuntimeContext());
    }

    @Test
    void leavesIsProvideRuntimeContextNullUnchanged() {
        var deploy = new RulesDeploy();

        RulesDeployMigrations.runtimeContext(deploy);

        assertNull(deploy.isProvideRuntimeContext());
    }

    @Test
    void movesInterceptingToEmptyAnnotation() {
        var deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Tpl");
        deploy.setAnnotationTemplateClassName("");

        RulesDeployMigrations.templateClass(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void movesInterceptingToNullAnnotation() {
        var deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Tpl");

        RulesDeployMigrations.templateClass(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void dropsInterceptingWhenAnnotationPresent() {
        var deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Old");
        deploy.setAnnotationTemplateClassName("com.example.New");

        RulesDeployMigrations.templateClass(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.New", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void doesNotMoveBlankInterceptingIntoAnnotation() {
        var deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName(" ");
        deploy.setAnnotationTemplateClassName("");

        RulesDeployMigrations.templateClass(deploy);

        // Blank intercepting is the empty-cleanup migrator's responsibility, not ours.
        assertEquals(" ", deploy.getInterceptingTemplateClassName());
        assertEquals("", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void leavesBothTemplateSlotsNullUntouched() {
        var deploy = new RulesDeploy();

        RulesDeployMigrations.templateClass(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertNull(deploy.getAnnotationTemplateClassName());
    }

    @Test
    void applyRunsBothTransforms() {
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);
        deploy.setInterceptingTemplateClassName("com.example.Tpl");

        RulesDeployMigrations.apply(deploy);

        assertNull(deploy.isProvideRuntimeContext());
        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
    }
}

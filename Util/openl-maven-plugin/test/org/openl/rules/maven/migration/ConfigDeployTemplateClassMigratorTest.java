package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;

class ConfigDeployTemplateClassMigratorTest {

    @Test
    void movesInterceptingToEmptyAnnotation() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Tpl");
        deploy.setAnnotationTemplateClassName("");

        ConfigDeployTemplateClassMigrator.transform(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void movesInterceptingToNullAnnotation() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Tpl");

        ConfigDeployTemplateClassMigrator.transform(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void dropsInterceptingWhenAnnotationPresent() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName("com.example.Old");
        deploy.setAnnotationTemplateClassName("com.example.New");

        ConfigDeployTemplateClassMigrator.transform(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.New", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void doesNotMoveBlankInterceptingIntoAnnotation() {
        RulesDeploy deploy = new RulesDeploy();
        deploy.setInterceptingTemplateClassName(" ");
        deploy.setAnnotationTemplateClassName("");

        ConfigDeployTemplateClassMigrator.transform(deploy);

        // Blank intercepting is the empty-cleanup migrator's responsibility, not ours.
        assertEquals(" ", deploy.getInterceptingTemplateClassName());
        assertEquals("", deploy.getAnnotationTemplateClassName());
    }

    @Test
    void leavesBothNullUntouched() {
        RulesDeploy deploy = new RulesDeploy();

        ConfigDeployTemplateClassMigrator.transform(deploy);

        assertNull(deploy.getInterceptingTemplateClassName());
        assertNull(deploy.getAnnotationTemplateClassName());
    }
}

package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

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
    void applyRunsEveryTransform() {
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);
        deploy.setInterceptingTemplateClassName("com.example.Tpl");
        deploy.setConfiguration(new HashMap<>(Map.of("jackson.defaultTypingMode",
                "EVERYTHING",
                "jackson.simpleClassNameAsTypingPropertyValue",
                "true")));

        RulesDeployMigrations.apply(deploy);

        assertNull(deploy.isProvideRuntimeContext());
        assertNull(deploy.getInterceptingTemplateClassName());
        assertEquals("com.example.Tpl", deploy.getAnnotationTemplateClassName());
        assertEquals(Map.of("jackson.defaultTypingMode", "NON_FINAL_AND_ENUMS", "jackson.jsonTypeInfoId", "NAME"),
                deploy.getConfiguration());
    }

    @Test
    void replacesEverythingTypingMode() {
        assertEquals(Map.of("jackson.defaultTypingMode", "NON_FINAL_AND_ENUMS"),
                jacksonTyping(Map.of("jackson.defaultTypingMode", " everything ")));
    }

    @Test
    void keepsOtherTypingModes() {
        assertEquals(Map.of("jackson.defaultTypingMode", "NON_FINAL"),
                jacksonTyping(Map.of("jackson.defaultTypingMode", "NON_FINAL")));
    }

    @Test
    void turnsSimpleClassNameIntoNameTypeInfoId() {
        assertEquals(Map.of("jackson.jsonTypeInfoId", "NAME"),
                jacksonTyping(Map.of("jackson.simpleClassNameAsTypingPropertyValue", "TRUE")));
        assertEquals(Map.of("jackson.jsonTypeInfoId", "NAME"),
                jacksonTyping(Map.of("jackson.simpleClassNameAsTypingPropertyValue",
                        "true",
                        "jackson.jsonTypeInfoId",
                        " CLASS ")));
    }

    @Test
    void keepsTypeInfoIdOtherThanClass() {
        assertEquals(Map.of("jackson.jsonTypeInfoId", "MINIMAL_CLASS"),
                jacksonTyping(Map.of("jackson.simpleClassNameAsTypingPropertyValue",
                        "true",
                        "jackson.jsonTypeInfoId",
                        "MINIMAL_CLASS")));
    }

    @Test
    void dropsSimpleClassNameThatIsNotTrue() {
        assertEquals(Map.of("jackson.jsonTypeInfoId", "CLASS"),
                jacksonTyping(Map.of("jackson.simpleClassNameAsTypingPropertyValue",
                        "false",
                        "jackson.jsonTypeInfoId",
                        "CLASS")));
        // the flag was read without trimming, so " true" meant false
        assertEquals(Map.of(), jacksonTyping(Map.of("jackson.simpleClassNameAsTypingPropertyValue", " true")));
    }

    @Test
    void leavesDeployWithoutConfigurationUntouched() {
        var deploy = new RulesDeploy();

        RulesDeployMigrations.jacksonTyping(deploy);

        assertNull(deploy.getConfiguration());
    }

    private static Map<String, Object> jacksonTyping(Map<String, Object> configuration) {
        var deploy = new RulesDeploy();
        deploy.setConfiguration(new HashMap<>(configuration));

        RulesDeployMigrations.jacksonTyping(deploy);

        return deploy.getConfiguration();
    }
}

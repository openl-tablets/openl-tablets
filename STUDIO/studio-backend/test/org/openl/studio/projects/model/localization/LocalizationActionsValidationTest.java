package org.openl.studio.projects.model.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Verifies that a key or a language is added with named keys and with every value given. */
class LocalizationActionsValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsAKeyAndALanguageWithTheirValues() {
        var actions = new LocalizationActions(List.of(
                new LocalizationAction.AddKey("note", Map.of("", "A note")),
                new LocalizationAction.AddLanguage("fr", Map.of("note", "Une note")),
                new LocalizationAction.AddLanguage("de", null)));

        assertTrue(validator.validate(actions).isEmpty());
    }

    @Test
    void writesALanguageOnlyAsTheEngineOpensItsFile() {
        var written = List.<LocalizationAction>of(
                new LocalizationAction.AddLanguage("de_LU", null),
                new LocalizationAction.AddLanguage("de__POSIX", null),
                new LocalizationAction.AddLanguage("es_419", null),
                new LocalizationAction.AddLanguage("", null));
        assertTrue(validator.validate(new LocalizationActions(written)).isEmpty());

        // On a file system that tells the cases apart the engine would never read these files. The case of the
        // country is the service's to require, as the engine names the file of the locale.
        var refused = List.<LocalizationAction>of(
                new LocalizationAction.AddLanguage("DE", null),
                new LocalizationAction.AddLanguage("de_", null));
        assertEquals(2, validator.validate(new LocalizationActions(refused)).size());
    }

    @Test
    void refusesAValueLeftOutAndAKeyWithoutAName() {
        var withoutValue = new HashMap<String, String>();
        withoutValue.put("", null);
        var actions = new LocalizationActions(List.of(
                // A key written without a value has no text: the rule returns the key.
                new LocalizationAction.AddKey("note", withoutValue),
                // A line of an empty key is no message.
                new LocalizationAction.AddLanguage("fr", Map.of(" ", "x"))));

        assertEquals(2, validator.validate(actions).size());
    }
}

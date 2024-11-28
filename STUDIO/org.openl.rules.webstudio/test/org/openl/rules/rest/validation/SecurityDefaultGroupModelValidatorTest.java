package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.BindingResult;

import org.openl.rules.rest.model.GroupSettingsModel;

@SpringJUnitConfig(classes = MockConfiguration.class)
public class SecurityDefaultGroupModelValidatorTest extends AbstractConstraintValidatorTest {

    @Test
    public void testValid() {
        GroupSettingsModel model = new GroupSettingsModel();
        model.setDefaultGroup("Some group");
        assertNull(validateAndGetResult(model));
    }

    @Test
    public void testValid1() {
        GroupSettingsModel model = new GroupSettingsModel();
        assertNull(validateAndGetResult(model));
    }

    @Test
    public void testNotValid1() {
        GroupSettingsModel model = new GroupSettingsModel();
        model.setDefaultGroup(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam eget erat in massa accumsan rhoncus.");

        BindingResult bindingResult = validateAndGetResult(model);
        assertEquals(1, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("defaultGroup",
                "The size must be between 0 and 65.",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam eget erat in massa accumsan rhoncus.",
                bindingResult.getFieldError("defaultGroup"));
    }

}

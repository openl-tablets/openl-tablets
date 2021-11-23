package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.rest.model.GroupSettingsModel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
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
            "Size must be between 0 and 50",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam eget erat in massa accumsan rhoncus.",
            bindingResult.getFieldError("defaultGroup"));
    }

}

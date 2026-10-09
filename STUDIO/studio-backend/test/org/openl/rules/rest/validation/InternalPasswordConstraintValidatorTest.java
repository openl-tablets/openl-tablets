package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.rest.model.InternalPasswordModel;

/**
 * The password of a new user when an external identity provider manages the credentials. {@link UsersValidatorTest}
 * covers the credentials that OpenL Studio stores.
 *
 * @author Yury Molchan
 */
class InternalPasswordConstraintValidatorTest {

    private final InternalPasswordConstraintValidator validator = new InternalPasswordConstraintValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    void withoutPassword_valid() {
        assertTrue(validator.isValid(null, context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "pass", "longer than twenty five characters"})
    void anyPassword_valid(String password) {
        assertTrue(validator.isValid(new InternalPasswordModel().setPassword(password), context));
    }
}

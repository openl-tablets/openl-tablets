package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * @author Yury Molchan
 */
class CommentValidatorTest {

    @Test
    void acceptsAnyCommentWithoutAPattern() {
        var validator = new CommentValidator("  ", "Start with a ticket");

        assertDoesNotThrow(() -> validator.validate("Any comment"));
        assertDoesNotThrow(() -> validator.validate(null));
    }

    @Test
    void refusesACommentThatDoesNotMatchThePattern() {
        var validator = new CommentValidator("EPBDS-\\d+ .*", "Start with a ticket");

        assertDoesNotThrow(() -> validator.validate("EPBDS-1 Fix rates"));
        var error = assertThrows(IllegalArgumentException.class, () -> validator.validate("Fix rates"));
        assertEquals("Start with a ticket", error.getMessage());
    }

    @Test
    void ignoresTheSpacesAroundThePatternAsTheScreensDo() {
        var validator = new CommentValidator(" EPBDS-\\d+ .* ", "Start with a ticket");

        assertDoesNotThrow(() -> validator.validate("EPBDS-1 Fix rates"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(" EPBDS-1 Fix rates "));
    }

    @Test
    void refusesACommentLongerThanARepositoryKeeps() {
        var validator = new CommentValidator(null, null);
        var longest = "a".repeat(CommentValidator.MAX_COMMENT_LENGTH);
        var tooLong = longest + "a";

        assertDoesNotThrow(() -> validator.validate(longest));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(tooLong));
    }
}

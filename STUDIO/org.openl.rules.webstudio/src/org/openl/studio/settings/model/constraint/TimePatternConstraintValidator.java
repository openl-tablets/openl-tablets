package org.openl.studio.settings.model.constraint;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

public class TimePatternConstraintValidator implements ConstraintValidator<TimePatternConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            // skip empty
            return true;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy " + value);
            // Seconds aren't obligatory.
            LocalDateTime dateTimeToTest = LocalDateTime.of(2020, 2, 22, 15, 16);
            Date date = Date.from(dateTimeToTest.atZone(ZoneId.systemDefault()).toInstant());

            String dateStr = format.format(date);
            Date parsedDate = format.parse(dateStr);

            return parsedDate.equals(date);
        } catch (Exception ignored) {
            return false;
        }
    }
}

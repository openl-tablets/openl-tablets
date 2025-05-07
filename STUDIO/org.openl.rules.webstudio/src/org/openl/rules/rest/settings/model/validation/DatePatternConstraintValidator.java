package org.openl.rules.rest.settings.model.validation;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

public class DatePatternConstraintValidator implements ConstraintValidator<DatePatternConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            // skip empty
            return true;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(value);
            LocalDate dateToTest = LocalDate.of(2020, 2, 22);
            Date date = Date.from(dateToTest.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            String dateStr = format.format(date);
            Date parsedDate = format.parse(dateStr);

            return parsedDate.equals(date);
        } catch (Exception ignored) {
            return false;
        }
    }
}

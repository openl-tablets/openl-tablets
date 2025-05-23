package org.openl.rules.rest.settings.model.validation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.webstudio.web.admin.DBSettings;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

public class DBConnectionConstraintValidator implements ConstraintValidator<DBConnectionConstraint, DBSettings> {

    private static final Logger LOG = LoggerFactory.getLogger(DBConnectionConstraintValidator.class);

    private static final Map<String, String> SQL_ERROR_MESSAGES;

    static {
        var properties = new HashMap<String, String>();
        try {
            var sqlErrorsFile = DBConnectionConstraintValidator.class.getResource("/sql-errors.properties");
            PropertiesUtils.load(sqlErrorsFile, properties::put);
        } catch (Exception e) {
            LOG.warn("Failed to load SQL error messages", e);
        }
        SQL_ERROR_MESSAGES = Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean isValid(DBSettings settings, ConstraintValidatorContext ctx) {
        if (StringUtils.isBlank(settings.getUrl())) {
            return true; // skip empty
        }
        return testConnection(settings, ctx);
    }

    private boolean testConnection(DBSettings settings, ConstraintValidatorContext ctx) {
        try (Connection connection = openConnection(settings)) {
            return connection.isValid(2);
        } catch (SQLException e) {
            var errorMessage = Optional.of(e.getErrorCode())
                    .map(this::explainSqlError)
                    .orElseGet(e::getMessage);
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
            return false;
        }
    }

    private Connection openConnection(DBSettings settings) throws SQLException {
        if (StringUtils.isBlank(settings.getUser())) {
            return DriverManager.getConnection(settings.getUrl());
        } else {
            return DriverManager.getConnection(settings.getUrl(), settings.getUser(), settings.getPassword());
        }
    }

    private String explainSqlError(int errorCode) {
        return SQL_ERROR_MESSAGES.get(Integer.toString(errorCode));
    }
}

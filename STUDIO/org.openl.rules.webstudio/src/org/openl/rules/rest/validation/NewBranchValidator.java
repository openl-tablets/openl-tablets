package org.openl.rules.rest.validation;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import jakarta.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.util.StringUtils;

/**
 * Validator for new branch name.
 *
 * @author Vladyslav Pikus
 */
@ParametersAreNonnullByDefault
public class NewBranchValidator implements Validator {

    private final BranchRepository repository;
    private final String customRegex;
    private final String customRegexError;

    public NewBranchValidator(BranchRepository repository,
                              @Nullable String customRegex,
                              @Nullable String customRegexError) {
        this.repository = repository;
        this.customRegex = customRegex;
        this.customRegexError = customRegexError;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == String.class;
    }

    @Override
    public void validate(@Nullable Object target, Errors errors) {
        Predicate<String> chain = newBranchName -> basicValidation(newBranchName, errors);
        chain = chain.and(newBranchName -> validateWithCustomPattern(newBranchName, errors));
        chain = chain.and(newBranchName -> validateWithRepository(newBranchName, errors));
        chain.test((String) target);
    }

    private boolean validateWithRepository(String newBranchName, Errors errors) {
        if (!repository.isValidBranchName(newBranchName)) {
            errors.reject("branch.name.invalid.4.message");
            return false;
        }
        try {
            for (String branch : repository.getBranches(null)) {
                if (branch.equalsIgnoreCase(newBranchName)) {
                    errors.reject("branch.name.exists.message", new Object[]{newBranchName}, null);
                    return false;
                } else if (newBranchName.startsWith(branch + "/")) {
                    errors.reject("branch.name.exists.1.message", new Object[]{newBranchName, branch}, null);
                    return false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private boolean validateWithCustomPattern(String newBranchName, Errors errors) {
        if (StringUtils.isBlank(customRegex)) {
            // just skip it
            return true;
        }
        try {
            if (!newBranchName.matches(customRegex)) {
                if (StringUtils.isBlank(customRegexError)) {
                    errors.reject("branch.name.invalid.pattern.message", new Object[]{customRegex}, null);
                } else {
                    errors.reject(customRegexError, customRegexError);
                }
                return false;
            }
        } catch (PatternSyntaxException e) {
            errors.reject("branch.pattern.invalid.message");
            return false;
        }
        return true;
    }

    private boolean basicValidation(@Nullable String newBranchName, Errors errors) {
        if (StringUtils.isBlank(newBranchName)) {
            errors.reject("branch.name.empty.message");
            return false;
        }
        if (!newBranchName.matches("[^\\\\:*?\"<>|{}~^\\s]*")) {
            errors.reject("branch.name.invalid.message");
            return false;
        }
        if (newBranchName.contains(".lock/") || newBranchName.endsWith(".lock")) {
            errors.reject("branch.name.invalid.3.message");
            return false;
        }
        if (!newBranchName.matches("^[^./].*[^./]")) {
            errors.reject("branch.name.invalid.2.message");
            return false;
        }
        if (!newBranchName.matches("(.(?<![./]{2}))+")) {
            errors.reject("branch.name.invalid.1.message");
            return false;
        }
        return true;
    }
}

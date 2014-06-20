package org.openl.rules.project.resolving;

/**
 * @author nsamatov.
 */
public interface PropertiesPatternValidator {
    void validate(String pattern) throws InvalidFileNamePatternException;
}

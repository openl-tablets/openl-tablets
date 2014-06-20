package org.openl.rules.project.resolving;

/**
 * @author nsamatov.
 */
public interface FileNamePatternValidator {
    void validate(String pattern) throws InvalidFileNamePatternException;
}

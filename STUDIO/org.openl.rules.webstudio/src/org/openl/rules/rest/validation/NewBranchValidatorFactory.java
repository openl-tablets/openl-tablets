package org.openl.rules.rest.validation;

import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Factory for {@link NewBranchValidator}.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class NewBranchValidatorFactory implements Function<BranchRepository, NewBranchValidator> {

    private final Environment environment;

    public NewBranchValidatorFactory(Environment environment) {
        this.environment = environment;
    }

    @Override
    public NewBranchValidator apply(BranchRepository branchRepository) {
        var key_prefix = Comments.REPOSITORY_PREFIX + branchRepository.getId();
        String customRegex = environment.getProperty(key_prefix + ".new-branch.regex");
        String customRegexError = environment.getProperty(key_prefix + ".new-branch.regex-error");
        return new NewBranchValidator(branchRepository, customRegex, customRegexError);
    }
}

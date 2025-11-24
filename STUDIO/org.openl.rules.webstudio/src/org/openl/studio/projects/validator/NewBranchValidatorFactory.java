package org.openl.studio.projects.validator;

import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;

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

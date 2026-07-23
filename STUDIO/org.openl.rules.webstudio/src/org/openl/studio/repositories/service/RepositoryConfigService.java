package org.openl.studio.repositories.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;

import org.openl.rules.webstudio.web.admin.GitRepositorySettings;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.studio.repositories.model.RepositoryConfigModel;
import org.openl.util.StringUtils;

/**
 * Reads the settings of a design repository that the project forms need — the new branch pattern and the
 * commit comment templates, together with the expressions they are validated against.
 *
 * <p>Access is never checked here: the caller authorizes the user, either on the repository or on a project
 * stored in it.
 *
 * @author Vladyslav Pikus
 */
@Service
@RequiredArgsConstructor
public class RepositoryConfigService {

    private final PropertyResolver propertyResolver;

    /**
     * Public configuration of the given repository.
     *
     * <p>A repository without branches has no branch rules. A repository that does not customize comments
     * has no comment expression, so any comment is accepted.
     *
     * @param repositoryId design repository id
     */
    public RepositoryConfigModel getConfig(String repositoryId) {
        var settings = new RepositoryConfiguration(repositoryId, propertyResolver).getSettings();
        return new RepositoryConfigModel(newBranch(settings), comment(settings));
    }

    private static RepositoryConfigModel.NewBranch newBranch(RepositorySettings settings) {
        if (settings instanceof GitRepositorySettings git) {
            return new RepositoryConfigModel.NewBranch(StringUtils.trimToNull(git.getNewBranchTemplate()),
                    StringUtils.trimToNull(git.getNewBranchRegex()),
                    StringUtils.trimToNull(git.getNewBranchRegexError()));
        }
        return null;
    }

    private static RepositoryConfigModel.Comment comment(RepositorySettings settings) {
        // The comment expression only applies while the repository customizes comments.
        var pattern = settings.isUseCustomComments()
                ? StringUtils.trimToNull(settings.getCommentValidationPattern())
                : null;
        var templates = new RepositoryConfigModel.Templates(StringUtils.trimToNull(settings.getDefaultCommentSave()),
                StringUtils.trimToNull(settings.getDefaultCommentCreate()),
                StringUtils.trimToNull(settings.getDefaultCommentCopiedFrom()),
                StringUtils.trimToNull(settings.getDefaultCommentRestoredFrom()));
        return new RepositoryConfigModel.Comment(pattern,
                pattern == null ? null : StringUtils.trimToNull(settings.getInvalidCommentMessage()),
                templates);
    }
}

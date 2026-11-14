package org.openl.rules.workspace;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The names of the settings of the repositories OpenL Studio connects to.
 *
 * <p>Every setting of a repository is named {@code repository.<id>.<setting>}, such as
 * {@code repository.design.uri}.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepositorySettingNames {

    /** The start of the name of every repository setting, followed by the id of the repository. */
    public static final String PREFIX = "repository.";
}

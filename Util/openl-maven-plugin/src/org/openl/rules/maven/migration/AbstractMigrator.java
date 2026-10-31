package org.openl.rules.maven.migration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A {@link Migrator} described by the id, the commit message and the description it is created with.
 *
 * @author Yury Molchan
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractMigrator implements Migrator {

    private final String id;
    private final String commitMessage;
    private final String description;
}

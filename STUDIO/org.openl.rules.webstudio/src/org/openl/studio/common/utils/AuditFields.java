package org.openl.studio.common.utils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;

/**
 * Copies the audit fields of a {@link FileData} — author, last-modified time (in the system zone) and
 * revision — onto a view-model builder, skipping any that are absent.
 */
public final class AuditFields {

    private AuditFields() {
    }

    public static void apply(FileData fileData,
                             Consumer<String> modifiedBy,
                             Consumer<ZonedDateTime> modifiedAt,
                             Consumer<String> revision) {
        Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).ifPresent(modifiedBy);
        Optional.ofNullable(fileData.getModifiedAt()).map(DateTimes::atSystemZone).ifPresent(modifiedAt);
        Optional.ofNullable(fileData.getVersion()).ifPresent(revision);
    }
}

package org.openl.studio.common.utils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;

/**
 * Copies the audit fields of a {@link FileData} onto a view-model builder, skipping any that are absent:
 * author and last-modified time (in the system zone), and the revision when the caller shows one.
 */
public final class AuditFields {

    private AuditFields() {
    }

    /**
     * Copies who changed the file and when.
     */
    public static void apply(FileData fileData,
                             Consumer<String> modifiedBy,
                             Consumer<ZonedDateTime> modifiedAt) {
        Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).ifPresent(modifiedBy);
        Optional.ofNullable(fileData.getModifiedAt()).map(DateTimes::atSystemZone).ifPresent(modifiedAt);
    }

    /**
     * Copies who changed the file and when, together with the revision the change produced in the repository
     * holding the file.
     */
    public static void apply(FileData fileData,
                             Consumer<String> modifiedBy,
                             Consumer<ZonedDateTime> modifiedAt,
                             Consumer<String> revision) {
        apply(fileData, modifiedBy, modifiedAt);
        Optional.ofNullable(fileData.getVersion()).ifPresent(revision);
    }
}

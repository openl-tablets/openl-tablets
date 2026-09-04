package org.openl.rules.repository.azure;

import com.azure.storage.blob.models.BlobItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class BlobEmulation {
    @Getter(AccessLevel.PACKAGE)
    private final BlobItem blobItem;
    @Getter(AccessLevel.PACKAGE)
    private final byte[] content;
}

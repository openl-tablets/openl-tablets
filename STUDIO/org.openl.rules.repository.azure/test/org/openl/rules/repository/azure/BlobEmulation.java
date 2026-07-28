package org.openl.rules.repository.azure;

import com.azure.storage.blob.models.BlobItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class BlobEmulation {
    private final BlobItem blobItem;
    private final byte[] content;

    BlobItem getBlobItem() {
        return blobItem;
    }

    byte[] getContent() {
        return content;
    }
}

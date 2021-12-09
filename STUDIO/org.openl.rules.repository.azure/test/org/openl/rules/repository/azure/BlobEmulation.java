package org.openl.rules.repository.azure;

import com.azure.storage.blob.models.BlobItem;

class BlobEmulation {
    private final BlobItem blobItem;
    private final byte[] content;

    BlobEmulation(BlobItem blobItem, byte[] content) {
        this.blobItem = blobItem;
        this.content = content;
    }

    BlobItem getBlobItem() {
        return blobItem;
    }

    byte[] getContent() {
        return content;
    }
}

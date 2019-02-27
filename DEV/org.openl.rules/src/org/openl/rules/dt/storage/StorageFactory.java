package org.openl.rules.dt.storage;

import org.openl.rules.dt.DTScale;

public class StorageFactory {

    public static IStorageBuilder makeStorageBuilder(int size, DTScale.RowScale scale) {

        int newSize = scale.getActualSize(size);

        StorageBuilder sb = new ObjectStorageBuilder(newSize);

        if (newSize == size) {
            return sb;
        }

        return new ScaleStorageBuilder(scale, sb);
    }

}

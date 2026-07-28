package org.openl.rules.dt.storage;

class ByteExtMappedStorage extends MappedStorage {

    private final byte[] bmap;

    ByteExtMappedStorage(int[] map, Object[] uniqueValues, IStorage storage, StorageInfo info) {
        super(uniqueValues, storage, info);
        var size = map.length;
        bmap = new byte[size];
        for (var i = 0; i < size; i++) {
            bmap[i] = (byte) (Byte.MAX_VALUE - map[i]);
        }
    }

    @Override
    public final int size() {
        return bmap.length;
    }

    @Override
    protected int mapIndex(int index) {
        return Byte.MAX_VALUE - bmap[index];
    }
}

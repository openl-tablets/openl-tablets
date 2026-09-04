package org.openl.rules.dt.storage;

class ShortMappedStorage extends MappedStorage {

    private final short[] bmap;

    ShortMappedStorage(int[] map, Object[] uniqueValues, IStorage storage, StorageInfo info) {
        super(uniqueValues, storage, info);
        var size = map.length;
        bmap = new short[size];
        for (var i = 0; i < size; i++) {
            bmap[i] = (short) map[i];
        }
    }

    @Override
    public final int size() {
        return bmap.length;
    }

    @Override
    protected int mapIndex(int index) {
        return bmap[index];
    }
}

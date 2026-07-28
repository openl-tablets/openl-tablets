package org.openl.rules.dt.storage;

class ShortExtMappedStorage extends MappedStorage {

    private final short[] bmap;

    ShortExtMappedStorage(int[] map, Object[] uniqueValues, IStorage storage, StorageInfo info) {
        super(uniqueValues, storage, info);
        var size = map.length;
        bmap = new short[size];
        for (var i = 0; i < size; i++) {
            bmap[i] = (short) (Short.MAX_VALUE - map[i]);
        }
    }

    @Override
    public final int size() {
        return bmap.length;
    }

    @Override
    protected int mapIndex(int index) {
        return Short.MAX_VALUE - bmap[index];
    }
}

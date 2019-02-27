package org.openl.rules.dt.storage;

class ShortExtMappedStorage extends MappedStorage {

    private short[] bmap;

    ShortExtMappedStorage(int[] map, Object[] uniqueValues, StorageInfo info) {
        super(uniqueValues, info);
        int size = map.length;
        bmap = new short[size];
        for (int i = 0; i < size; i++) {
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

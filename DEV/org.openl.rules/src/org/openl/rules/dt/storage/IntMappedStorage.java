package org.openl.rules.dt.storage;

class IntMappedStorage extends MappedStorage {

    private int[] map;

    IntMappedStorage(int[] map, Object[] uniqueValues, StorageInfo info) {
        super(uniqueValues, info);
        this.map = map;
    }

    @Override
    public final int size() {
        return map.length;
    }

    @Override
    protected int mapIndex(int index) {
        return map[index];
    }

}

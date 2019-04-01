package org.openl.rules.dt.storage;

import java.util.Map;

public class ObjectStorageBuilder extends StorageBuilder<Object> {

    private static final int MIN_MAPPED_SIZE = 16;
    ObjectStorage storage;

    ObjectStorageBuilder(int size) {
        this.storage = new ObjectStorage(size);
    }

    @Override
    public void writeValue(Object value, int index) {
        storage.setValue(index, value);
    }

    @Override
    public void writeSpace(int index) {
        storage.setSpace(index);

    }

    @Override
    public void writeElse(int index) {
        storage.setElse(index);
    }

    @Override
    public void writeFormula(Object formula, int index) {
        storage.setFormula(index, formula);
    }

    @Override
    public IStorage<Object> optimizeAndBuild() {
        storage.setInfo(info);
        return shouldUseMappedStorage() ? makeMappedStorage() : storage;
    }

    private IStorage<Object> makeMappedStorage() {

        int size = storage.size();

        int[] map = new int[size];

        Object[] uniqueValues = new Object[info.getTotalNumberOfUniqueValues()];

        int firstFormulaIndex = info.getUniqueIndex().size();

        int spaceIndex = info.getNumberOfFormulas() + firstFormulaIndex;

        int elseIndex = info.getNumberOfSpaces() == 0 ? spaceIndex : spaceIndex + 1;

        for (Map.Entry<Object, Integer> e : info.getUniqueIndex().entrySet()) {
            uniqueValues[e.getValue()] = e.getKey();
        }

        int formulaCnt = 0;

        for (int i = 0; i < size; i++) {
            if (storage.isElse(i)) {
                map[i] = elseIndex;
                uniqueValues[elseIndex] = IStorage.StorageType.ELSE;
            } else if (storage.isSpace(i)) {
                map[i] = spaceIndex;
                uniqueValues[spaceIndex] = null;
            } else if (storage.isFormula(i)) {
                map[i] = firstFormulaIndex + formulaCnt++;
                uniqueValues[map[i]] = storage.getValue(i);
            } else // value
            {
                Object value = storage.getValue(i);
                map[i] = info.getUniqueIndex().get(value);
            }

        }

        int mapMaxValue = uniqueValues.length - 1;

        if (mapMaxValue <= Byte.MAX_VALUE) {
            return new ByteMappedStorage(map, uniqueValues, info);
        }

        if (mapMaxValue <= Byte.MAX_VALUE - Byte.MIN_VALUE) {
            return new ByteExtMappedStorage(map, uniqueValues, info);
        }

        if (mapMaxValue <= Short.MAX_VALUE) {
            return new ShortMappedStorage(map, uniqueValues, info);
        }

        if (mapMaxValue <= Short.MAX_VALUE - Short.MIN_VALUE) {
            return new ShortExtMappedStorage(map, uniqueValues, info);
        }

        return new IntMappedStorage(map, uniqueValues, info);
    }

    private boolean shouldUseMappedStorage() {

        if (size() < MIN_MAPPED_SIZE) {
            return false;
        }

        double uniqueValues = info.getTotalNumberOfUniqueValues();

        return (uniqueValues / size() < 0.7);
    }

    @Override
    protected void checkMinMax(Object loadedValue) {
        // DO NOTHING
    }

    @Override
    public int size() {
        return storage.size();
    }

}

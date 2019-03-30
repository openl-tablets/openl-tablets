package org.openl.rules.dt.storage;

public interface IStorageBuilder<T> {

    IStorage<T> optimizeAndBuild();

    void writeObject(Object loadedValue, int index);

    int size();

}
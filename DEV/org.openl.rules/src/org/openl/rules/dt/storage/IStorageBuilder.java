package org.openl.rules.dt.storage;

public interface IStorageBuilder {

    IStorage optimizeAndBuild();

    void writeObject(Object loadedValue, int index);

    int size();

}

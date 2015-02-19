package org.openl.rules.dt2.storage;

public interface IStorageBuilder<T> {

	public abstract IStorage<T> optimizeAndBuild();

	public abstract void writeObject(Object loadedValue, int index);

	public abstract int size();

}
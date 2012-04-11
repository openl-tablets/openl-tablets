package org.openl.util;

public interface RemovableHandler<K, T> {
	

	T getHandledobject(); //loads and returns required object
	void removeHandledObject(); //cleanup
	
	K getKey();  // returns a key identifying the object, the key must be "persistent", i.e. it should exist even if the handled object is null

}

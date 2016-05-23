/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.base;

import org.openl.util.ASelector;

/**
 * @author snshor
 * 
 *         Wow, almost everything has a name.
 */
public interface INamedThing {

    static class NameSelector<T extends INamedThing> extends ASelector<T> {
    	String value;
        public NameSelector(String value) {
        	this.value = value;
        }

		@Override
		public boolean select(T obj) {
			return value.equals(obj.getName());
		}
    }

    static public INamedThing[] EMPTY = {};

    static public int SHORT = 0;
    static public int REGULAR = 1;
    static public int LONG = 2;

    String getDisplayName(int mode);

    String getName();

}

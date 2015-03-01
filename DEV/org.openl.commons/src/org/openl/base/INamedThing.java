/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.base;

import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;

/**
 * @author snshor
 * 
 *         Wow, almost everything has a name.
 */
public interface INamedThing {

    static class NameConverter<T extends INamedThing> extends AStringConvertor<INamedThing> {
        @Override
        public String getStringValue(INamedThing nt) {
            return nt.getName();
        }
    }

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

    static class Tool {
        public static INamedThing find(INamedThing[] ary, String name) {
            for (INamedThing namedThing : ary) {
                if (namedThing.getName().equals(name)) {
                    return namedThing;
                }
            }
            return null;
        }
    }

    static public INamedThing[] EMPTY = {};

    static public int SHORT = 0;
    static public int REGULAR = 1;
    static public int LONG = 2;

    NameConverter<INamedThing> NAME_CONVERTOR = new NameConverter<INamedThing>();

    String getDisplayName(int mode);

    String getName();

}

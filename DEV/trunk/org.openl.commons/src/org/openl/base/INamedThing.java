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
 * Wow, almost everything has a name.
 */
public interface INamedThing {

    class NameConverter<T extends INamedThing> extends AStringConvertor<INamedThing> {
        @Override
        public String getStringValue(INamedThing nt) {
            return nt.getName();
        }
    }

    class NameSelector extends ASelector.StringValueSelector<INamedThing> {
        public NameSelector(String value) {
            super(value, NAME_CONVERTOR);
        }
    }

    class Tool {
        public static INamedThing find(INamedThing[] ary, String name) {
            for (INamedThing namedThing : ary) {
                if (namedThing.getName().equals(name)) {
                    return namedThing;
                }
            }
            return null;
        }
    }

    INamedThing[] EMPTY = {};

    int SHORT = 0;
    int REGULAR = 1;
    int LONG = 2;

    NameConverter<INamedThing> NAME_CONVERTOR = new NameConverter<INamedThing>();

    String getDisplayName(int mode);

    String getName();

}

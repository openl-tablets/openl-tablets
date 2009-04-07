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

    static class NameConverter<T extends INamedThing> extends AStringConvertor<INamedThing> {

        @Override
        public String getStringValue(INamedThing nt) {
            return nt.getName();
        }
    }

    public static class NameSelector extends ASelector.StringValueSelector<INamedThing> {
        public NameSelector(String value) {
            super(value, NAME_CONVERTOR);
        }
    }

    static public class Tool {
        static public INamedThing find(INamedThing[] ary, String name) {
            for (int i = 0; i < ary.length; i++) {
                if (ary[i].getName().equals(name)) {
                    return ary[i];
                }
            }
            return null;
        }
    }

    public static final INamedThing[] EMPTY = {};

    static public final int SHORT = 0, REGULAR = 1, LONG = 2;

    public static final NameConverter<INamedThing> NAME_CONVERTOR = new NameConverter<INamedThing>();

    public String getDisplayName(int mode);

    public String getName();

}

/**
 * Created May 5, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public abstract class GroupOperator {

    static public class AND extends GroupOperator {
        @Override
        public String getName() {
            return "AND";
        }

        @Override
        public boolean isGroup() {
            return false;
        }

        @Override
        public boolean op(boolean b1, boolean b2) {
            return b1 && b2;
        }
    }

    static public class GroupAND extends GroupOperator {
        @Override
        public String getName() {
            return "-AND NEXT GROUP-";
        }

        @Override
        public boolean isGroup() {
            return true;
        }

        @Override
        public boolean op(boolean b1, boolean b2) {
            return b1 && b2;
        }
    }

    static public class GroupOR extends GroupOperator {
        @Override
        public String getName() {
            return "-OR NEXT GROUP-";
        }

        @Override
        public boolean isGroup() {
            return true;
        }

        @Override
        public boolean op(boolean b1, boolean b2) {
            return b1 || b2;
        }
    }

    static public class OR extends GroupOperator {
        @Override
        public String getName() {
            return "OR";
        }

        @Override
        public boolean isGroup() {
            return false;
        }

        @Override
        public boolean op(boolean b1, boolean b2) {
            return b1 || b2;
        }
    }

    static public GroupOperator[] list = { new AND(), new OR(), new GroupOR(), new GroupAND() };

    static public String[] names = { list[0].getName(), list[1].getName(), list[2].getName(), list[3].getName() };

    /**
     * @param gopID
     * @return
     */
    public static GroupOperator find(String gopID) {
        if (gopID == null) {
            return list[0];
        }
        for (int i = 0; i < names.length; i++) {
            if (gopID.equals(names[i])) {
                return list[i];
            }
        }

        throw new RuntimeException("Operator not found: " + gopID);
    }

    public abstract String getName();

    public abstract boolean isGroup();

    public abstract boolean op(boolean b1, boolean b2);

}

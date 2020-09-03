/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

/**
 * @author snshor
 *
 */
public abstract class ASelector<T> implements ISelector<T> {

    static class ANDSelector<T> extends BoolBinSelector<T> {
        public ANDSelector(ISelector<T> sel1, ISelector<T> sel2) {
            super(sel1, sel2);
        }

        @Override
        public boolean select(T obj) {
            if (sel1.select(obj)) {
                return sel2.select(obj);
            }
            return false;
        }
    }

    /**
     *
     * @author snshor Base class for binary boolean operators
     */
    abstract static class BoolBinSelector<T> extends ASelector<T> {
        ISelector<T> sel1;
        ISelector<T> sel2;

        protected BoolBinSelector(ISelector<T> sel1, ISelector<T> sel2) {
            this.sel1 = sel1;
            this.sel2 = sel2;
        }

        @Override
        protected boolean equalsSelector(ASelector<T> sel) {
            BoolBinSelector<?> x;
            if (sel instanceof BoolBinSelector<?>) {
                x = (BoolBinSelector<?>) sel;
            } else {
                return false;
            }
            return sel1.equals(x.sel1) && sel2.equals(x.sel2);
        }

        @Override
        protected int redefinedHashCode() {
            return sel1.hashCode() + sel2.hashCode();
        }

    }

    static class NOTSelector<T> extends ASelector<T> {
        ISelector<T> is;

        public NOTSelector(ISelector<T> is) {
            this.is = is;
        }

        @Override
        protected boolean equalsSelector(ASelector<T> sel) {
            return is.equals(((NOTSelector<?>) sel).is);
        }

        @Override
        protected int redefinedHashCode() {
            return is.hashCode();
        }

        @Override
        public boolean select(T obj) {
            return !is.select(obj);
        }

    }

    public static class ObjectSelector<T> extends ASelector<T> {
        T myobj;

        public ObjectSelector(T obj) {
            this.myobj = obj;
        }

        @Override
        protected boolean equalsSelector(ASelector<T> sel) {
            return select(((ObjectSelector<T>) sel).myobj);
        }

        @Override
        protected int redefinedHashCode() {
            return myobj == null ? 0 : myobj.hashCode();
        }

        @Override
        public boolean select(T obj) {
            if (myobj == obj) {
                return true;
            }
            if (myobj == null) {
                return false;
            }
            return myobj.equals(obj);
        }

    }

    static class ORSelector<T> extends BoolBinSelector<T> {
        public ORSelector(ISelector<T> sel1, ISelector<T> sel2) {
            super(sel1, sel2);
        }

        @Override
        public boolean select(T obj) {
            if (sel1.select(obj)) {
                return true;
            }
            return sel2.select(obj);
        }
    }

    public static class StringValueSelector<T> extends ASelector<T> {
        String value;
        AStringConvertor<T> convertor;

        public StringValueSelector(String value, AStringConvertor<T> convertor) {
            this.value = value;
            this.convertor = convertor;
        }

        @Override
        protected boolean equalsSelector(ASelector<T> sel) {
            StringValueSelector<?> svs = (StringValueSelector<?>) sel;
            return value.equals(svs.value) && convertor.equals(svs.convertor);
        }

        @Override
        protected int redefinedHashCode() {
            return value.hashCode() * 37 + convertor.hashCode();
        }

        @Override
        public boolean select(T obj) {
            return value.equals(convertor.getStringValue(obj));
        }
    }

    static class XORSelector<T> extends BoolBinSelector<T> {
        public XORSelector(ISelector<T> sel1, ISelector<T> sel2) {
            super(sel1, sel2);
        }

        @Override
        public boolean select(T obj) {
            return sel1.select(obj) ^ sel2.select(obj);
        }
    }

    public static <T> ISelector<T> selectObject(T obj) {
        return new ObjectSelector<>(obj);
    }

    @Override
    public ISelector<T> and(ISelector<T> isel) {
        return new ANDSelector<>(this, isel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return equalsSelector((ASelector<T>) obj);
    }

    protected boolean equalsSelector(ASelector<T> sel) {
        return sel == this;
    }

    @Override
    public int hashCode() {
        return redefinedHashCode();
    }

    @Override
    public ISelector<T> not() {
        return new NOTSelector<>(this);
    }

    @Override
    public ISelector<T> or(ISelector<T> isel) {
        return new ORSelector<>(this, isel);
    }

    protected int redefinedHashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public ISelector<T> xor(ISelector<T> isel) {
        return new XORSelector<>(this, isel);
    }

}

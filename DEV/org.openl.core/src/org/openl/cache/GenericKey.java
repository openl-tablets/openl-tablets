package org.openl.cache;

/**
 * This is immutable object for using it as a key. Build key by array of objects. This is instance controlled class. For
 * instantiate class use static factory method "getInstance" [EJ1].
 *
 */
public class GenericKey {
    private static final GenericKey NULL_KEY = new GenericKey();

    private GenericKey() {
    }

    /**
     * Returns a key instance for this objects
     */
    public static GenericKey getInstance(Object object1, Object object2) {
        if (object1 == null) {
            if (object2 == null) {
                return NULL_KEY;
            }
            return new Single2Key(object2);
        } else if (object2 == null) {
            return new Single1Key(object1);
        } else {
            return new TupleKey(object1, object2);
        }
    }

    private static class SingleKey extends GenericKey {
        Object object;

        SingleKey(Object object) {
            assert object != null;
            this.object = object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SingleKey singleKey = (SingleKey) o;

            return object.equals(singleKey.object);
        }

        @Override
        public int hashCode() {
            return object.hashCode();
        }
    }

    private static class TupleKey extends GenericKey {
        Object object1;
        Object object2;

        TupleKey(Object object1, Object object2) {
            assert object1 != null && object2 != null;
            this.object1 = object1;
            this.object2 = object2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TupleKey toupleKey = (TupleKey) o;

            if (!object1.equals(toupleKey.object1)) {
                return false;
            }
            return object2.equals(toupleKey.object2);
        }

        @Override
        public int hashCode() {
            int result = object1.hashCode();
            result = 31 * result + object2.hashCode();
            return result;
        }
    }

    private static class Single1Key extends SingleKey {
        Single1Key(Object object) {
            super(object);
        }
    }

    private static class Single2Key extends SingleKey {
        Single2Key(Object object) {
            super(object);
        }
    }
}
